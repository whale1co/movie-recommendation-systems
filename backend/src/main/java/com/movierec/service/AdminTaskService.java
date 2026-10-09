package com.movierec.service;

import com.movierec.common.PageResponse;
import com.movierec.entity.AdminTask;
import com.movierec.entity.AdminTaskError;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface AdminTaskService {
    AdminTask submitCsvImport(Long operatorId, MultipartFile file);

    AdminTask submitPosterDownload(Long operatorId);

    Map<String, Object> posterStatus();

    PageResponse<AdminTask> list(long current, long size, String status, String taskType);

    AdminTask get(Long taskId);

    List<AdminTaskError> errors(Long taskId);

    AdminTask cancel(Long taskId);

    void delete(Long taskId);
}
