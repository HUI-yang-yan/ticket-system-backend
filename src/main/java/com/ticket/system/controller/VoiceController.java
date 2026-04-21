package com.ticket.system.controller;

import com.ticket.system.common.result.Result;
import com.ticket.system.dto.request.VoiceRecognizeRequestDTO;
import com.ticket.system.dto.response.AiChatResponseDTO;
import com.ticket.system.service.VoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 语音识别控制器
 * 支持音频文件上传，自动识别语音内容并触发 AI 对话
 */
@Slf4j
@RestController
@RequestMapping("/ai/voice")
@RequiredArgsConstructor
@Tag(name = "语音识别", description = "语音识别、AI 对话（多模态）")
public class VoiceController {

    private final VoiceService voiceService;

    /**
     * 语音识别 + AI 对话
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "语音识别对话", description = "上传语音文件，自动识别内容并返回 AI 对话结果")
    public Result<AiChatResponseDTO> voiceChat(
            @RequestPart("audio") MultipartFile audio,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "autoQuery", defaultValue = "true") Boolean autoQuery) {

        log.info("Voice chat request - filename: {}, size: {}, sessionId: {}",
                audio.getOriginalFilename(), audio.getSize(), sessionId);

        VoiceRecognizeRequestDTO request = new VoiceRecognizeRequestDTO();
        request.setAudio(audio);
        request.setSessionId(sessionId);
        request.setAutoQuery(autoQuery);

        AiChatResponseDTO response = voiceService.recognizeAndChat(request);
        return Result.success(response);
    }

    /**
     * 语音识别 + AI 流式对话
     */
    @PostMapping(value = "/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "语音识别流式对话", description = "上传语音文件，识别后流式返回 AI 对话结果")
    public Flux<String> voiceChatStream(
            @RequestPart("audio") MultipartFile audio,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "autoQuery", defaultValue = "true") Boolean autoQuery) {

        log.info("Voice chat stream request - filename: {}, size: {}, sessionId: {}",
                audio.getOriginalFilename(), audio.getSize(), sessionId);

        VoiceRecognizeRequestDTO request = new VoiceRecognizeRequestDTO();
        request.setAudio(audio);
        request.setSessionId(sessionId);
        request.setAutoQuery(autoQuery);

        return voiceService.recognizeAndChatStream(request);
    }

    /**
     * 仅识别语音（不进行 AI 对话）
     */
    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "仅语音识别", description = "上传语音文件，仅返回识别出的文字内容")
    public Result<Map<String, String>> recognizeOnly(@RequestPart("audio") MultipartFile audio) {
        log.info("Recognize only - filename: {}, size: {}",
                audio.getOriginalFilename(), audio.getSize());

        try {
            String text = voiceService.recognizeOnly(audio.getBytes(), audio.getOriginalFilename());
            if (text != null && !text.isBlank()) {
                return Result.success(Map.of("text", text, "success", "true"));
            } else {
                return Result.success(Map.of("text", "", "success", "false", "error", "识别失败"));
            }
        } catch (Exception e) {
            log.error("Recognize only error", e);
            return Result.success(Map.of("text", "", "success", "false", "error", e.getMessage()));
        }
    }
}