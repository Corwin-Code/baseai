package com.cloud.baseai.application.chat.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 提交反馈命令
 */
public record SubmitFeedbackCommand(
        @NotNull(message = "评分不能为空")
        @Min(value = 1, message = "评分不能小于1")
        @Max(value = 5, message = "评分不能大于5")
        Integer rating,

        @Size(max = 1000, message = "评论内容不能超过1000字符")
        String comment,

        @NotNull(message = "用户ID不能为空")
        Long userId
) {
}