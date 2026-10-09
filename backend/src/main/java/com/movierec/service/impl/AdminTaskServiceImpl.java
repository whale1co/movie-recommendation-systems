package com.movierec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movierec.common.PageResponse;
import com.movierec.entity.AdminTask;
import com.movierec.entity.AdminTaskError;
import com.movierec.exception.BusinessException;
import com.movierec.exception.ResourceNotFoundException;
import com.movierec.mapper.AdminTaskErrorMapper;
import com.movierec.mapper.AdminTaskMapper;
import com.movierec.service.AdminTaskService;
import com.movierec.service.CrawlService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import com.movierec.mapper.MovieMapper;
import com.movierec.entity.Movie;

@Service
public class AdminTaskServiceImpl implements AdminTaskService {
    private static final String PENDING = "PENDING";
    private static final String RUNNING = "RUNNING";
    private static final String SUCCESS = "SUCCESS";
    private static final String PARTIAL = "PARTIAL";
    private static final String FAILED = "FAILED";
    private static final String CANCELLED = "CANCELLED";

    private final AdminTaskMapper taskMapper;
    private final MovieMapper movieMapper;
    private final AdminTaskErrorMapper errorMapper;
    private final CrawlService crawlService;
    private final ObjectMapper objectMapper;
    private final ThreadPoolTaskExecutor executor;
    private final Map<Long, Future<?>> runningTasks = new ConcurrentHashMap<>();
    private final Object submissionLock = new Object();
    @Value("${movie.poster-dir:./posters}")
    private String posterDir;
    @Value("${movie.import-dir:${java.io.tmpdir}/movierec-imports}")
    private String importDir;

    public AdminTaskServiceImpl(AdminTaskMapper taskMapper, MovieMapper movieMapper, AdminTaskErrorMapper errorMapper,
                                CrawlService crawlService, ObjectMapper objectMapper,
                                @Qualifier("adminTaskExecutor") ThreadPoolTaskExecutor executor) {
        this.taskMapper = taskMapper;
        this.movieMapper = movieMapper;
        this.errorMapper = errorMapper;
        this.crawlService = crawlService;
        this.objectMapper = objectMapper;
        this.executor = executor;
    }

    @Override
    public AdminTask submitCsvImport(Long operatorId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException(HttpStatus.BAD_REQUEST, "请选择 CSV 文件");
        if (file.getSize() > 50L * 1024 * 1024) throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "CSV 文件不能超过 50 MB");
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase(java.util.Locale.ROOT).endsWith(".csv")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "仅支持 .csv 文件");
        }
        Path storedFile;
        try {
            Path directory = Path.of(importDir).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            storedFile = Files.createTempFile(directory, "admin-import-", ".csv");
            file.transferTo(storedFile);
            try (var reader = Files.newBufferedReader(storedFile, java.nio.charset.StandardCharsets.UTF_8)) {
                String header = reader.readLine();
                if (header == null || header.split(",", -1).length < 18) {
                    Files.deleteIfExists(storedFile);
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "CSV 表头格式不正确，需包含电影数据的 18 列");
                }
            }
        } catch (BusinessException ex) { throw ex;
        } catch (IOException ex) { throw new BusinessException(HttpStatus.BAD_REQUEST, "无法读取 CSV 文件，请确认文件使用 UTF-8 编码"); }
        synchronized (submissionLock) {
            try { rejectDuplicate(); }
            catch (RuntimeException ex) {
                try { Files.deleteIfExists(storedFile); } catch (IOException ignored) { }
                throw ex;
            }
            AdminTask task = newTask("CSV_IMPORT", operatorId, Map.of("fileName", safeFileName(originalName), "storedPath", storedFile.toString()), 0);
            taskMapper.insert(task);
            runningTasks.put(task.getId(), start(task.getId()));
            return taskMapper.selectById(task.getId());
        }
    }

    @Override
    public AdminTask submitPosterDownload(Long operatorId) {
        synchronized (submissionLock) {
            rejectDuplicate();
            long total = movieMapper.selectCount(new LambdaQueryWrapper<Movie>().isNotNull(Movie::getPosterUrl)
                    .ne(Movie::getPosterUrl, "").likeRight(Movie::getPosterUrl, "http"));
            AdminTask task = newTask("POSTER_DOWNLOAD", operatorId, Map.of(), Math.toIntExact(Math.min(Integer.MAX_VALUE, total)));
            taskMapper.insert(task);
            runningTasks.put(task.getId(), start(task.getId()));
            return taskMapper.selectById(task.getId());
        }
    }

    @Override
    public Map<String, Object> posterStatus() {
        Path directory = Path.of(posterDir).toAbsolutePath().normalize();
        long files = 0, bytes = 0;
        if (Files.isDirectory(directory)) {
            try (var stream = Files.list(directory)) {
                for (Path file : stream.filter(Files::isRegularFile).toList()) { files++; bytes += Files.size(file); }
            } catch (IOException ex) { throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "读取海报目录状态失败"); }
        }
        long local = movieMapper.selectCount(new LambdaQueryWrapper<Movie>().likeRight(Movie::getPosterUrl, "/api/posters/"));
        long remote = movieMapper.selectCount(new LambdaQueryWrapper<Movie>().isNotNull(Movie::getPosterUrl).likeRight(Movie::getPosterUrl, "http"));
        long total = movieMapper.selectCount(null);
        return Map.of("directory", directory.toString(), "fileCount", files, "totalBytes", bytes,
                "localizedMovies", local, "remoteMovies", remote, "missingMovies", Math.max(0, total - local - remote));
    }

    private String safeFileName(String name) {
        String leaf = Path.of(name.replace('\\', '/')).getFileName().toString();
        return leaf.length() > 120 ? leaf.substring(leaf.length() - 120) : leaf;
    }

    private void rejectDuplicate() {
        long active = taskMapper.selectCount(new LambdaQueryWrapper<AdminTask>()
                .in(AdminTask::getStatus, PENDING, RUNNING)
                .in(AdminTask::getTaskType, "CSV_IMPORT", "POSTER_DOWNLOAD"));
        if (active > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "已有数据任务正在执行，请等待任务完成");
        }
    }

    private AdminTask newTask(String type, Long operatorId, Map<String, Object> params, int total) {
        AdminTask task = new AdminTask();
        task.setTaskType(type);
        task.setStatus(PENDING);
        task.setCreatedBy(operatorId);
        task.setTotalCount(total);
        task.setSuccessCount(0);
        task.setFailedCount(0);
        try {
            task.setParamsJson(objectMapper.writeValueAsString(params));
        } catch (JsonProcessingException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "任务参数序列化失败");
        }
        return task;
    }

    private Future<?> start(Long taskId) {
        if (executor == null) {
            throw new IllegalStateException("任务线程池未初始化");
        }
        return executor.submit(() -> runTask(taskId));
    }

    private void runTask(Long taskId) {
        AdminTask task = taskMapper.selectById(taskId);
        if (task == null || CANCELLED.equals(task.getStatus())) return;
        task.setStatus(RUNNING);
        task.setStartedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = objectMapper.readValue(task.getParamsJson(), Map.class);
            Map<String, Integer> result;
            if ("POSTER_DOWNLOAD".equals(task.getTaskType())) {
                result = crawlService.fetchPosters();
            } else {
                Path csvPath = Path.of((String) params.get("storedPath"));
                result = crawlService.importFromCsv(csvPath);
            }
            AdminTask current = taskMapper.selectById(taskId);
            if (current == null || CANCELLED.equals(current.getStatus())) return;
            int success = result.entrySet().stream().filter(e -> !e.getKey().contains("失败"))
                    .mapToInt(e -> e.getValue() == null ? 0 : Math.max(0, e.getValue())).sum();
            int failed = result.entrySet().stream().filter(e -> e.getKey().contains("失败"))
                    .mapToInt(e -> e.getValue() == null ? 0 : Math.max(0, e.getValue())).sum();
            if ("POSTER_DOWNLOAD".equals(task.getTaskType())) {
                success = value(result, "成功");
                failed = value(result, "失败");
            }
            current.setSuccessCount(success);
            current.setFailedCount(failed);
            current.setTotalCount(Math.max(current.getTotalCount(), success + failed));
            current.setStatus(failed == 0 ? SUCCESS : success == 0 ? FAILED : PARTIAL);
            String failureSummary = result.entrySet().stream().filter(e -> e.getKey().contains("失败") && e.getValue() != null && e.getValue() > 0)
                    .map(e -> e.getKey() + "=" + e.getValue()).reduce((a, b) -> a + "; " + b).orElse("");
            current.setErrorMessage(failed == 0 ? null : "有 " + failed + " 条记录处理失败" + (failureSummary.isBlank() ? "" : "（" + failureSummary + "）"));
            current.setFinishedAt(LocalDateTime.now());
            taskMapper.updateById(current);
            if (failed > 0) recordError(taskId, "TASK_ITEM", current.getErrorMessage());
        } catch (Exception ex) {
            AdminTask current = taskMapper.selectById(taskId);
            if (current != null && !CANCELLED.equals(current.getStatus())) {
                current.setStatus(FAILED);
                current.setErrorMessage(message(ex));
                current.setFinishedAt(LocalDateTime.now());
                taskMapper.updateById(current);
                recordError(taskId, "TASK_EXECUTION", current.getErrorMessage());
            }
        } finally {
            try {
                if ("CSV_IMPORT".equals(task.getTaskType())) {
                    @SuppressWarnings("unchecked") Map<String, Object> params = objectMapper.readValue(task.getParamsJson(), Map.class);
                    Files.deleteIfExists(Path.of((String) params.get("storedPath")));
                }
            } catch (Exception ignored) { }
            runningTasks.remove(taskId);
        }
    }

    private int value(Map<String, Integer> result, String key) {
        return result == null || result.get(key) == null ? 0 : Math.max(0, result.get(key));
    }

    private String message(Exception ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private void recordError(Long taskId, String type, String message) {
        AdminTaskError error = new AdminTaskError();
        error.setTaskId(taskId);
        error.setErrorType(type);
        error.setErrorMessage(message);
        errorMapper.insert(error);
    }

    @Override
    public PageResponse<AdminTask> list(long current, long size, String status, String taskType) {
        Page<AdminTask> page = new Page<>(current, size);
        LambdaQueryWrapper<AdminTask> wrapper = new LambdaQueryWrapper<AdminTask>()
                .eq(status != null && !status.isBlank(), AdminTask::getStatus, status)
                .eq(taskType != null && !taskType.isBlank(), AdminTask::getTaskType, taskType)
                .orderByDesc(AdminTask::getCreatedAt);
        Page<AdminTask> result = taskMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public AdminTask get(Long taskId) {
        AdminTask task = taskMapper.selectById(taskId);
        if (task == null) throw new ResourceNotFoundException("任务不存在");
        return task;
    }

    @Override
    public List<AdminTaskError> errors(Long taskId) {
        get(taskId);
        return errorMapper.selectList(new LambdaQueryWrapper<AdminTaskError>()
                .eq(AdminTaskError::getTaskId, taskId).orderByAsc(AdminTaskError::getId));
    }

    @Override
    public AdminTask cancel(Long taskId) {
        AdminTask task = get(taskId);
        if (SUCCESS.equals(task.getStatus()) || PARTIAL.equals(task.getStatus()) || FAILED.equals(task.getStatus()) || CANCELLED.equals(task.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "当前任务不能取消");
        }
        task.setStatus(CANCELLED);
        task.setErrorMessage("任务已由管理员取消");
        task.setFinishedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        Future<?> future = runningTasks.remove(taskId);
        if (future != null) future.cancel(true);
        return taskMapper.selectById(taskId);
    }

    @Override
    public void delete(Long taskId) {
        AdminTask task = get(taskId);
        if (PENDING.equals(task.getStatus()) || RUNNING.equals(task.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "运行中的任务不能删除，请先取消任务");
        }
        taskMapper.deleteById(taskId);
    }
}
