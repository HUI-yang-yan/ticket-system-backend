package com.ticket.system.controller;

import com.ticket.system.common.result.Result;
import com.ticket.system.dto.request.ContactDTO;
import com.ticket.system.entity.Contact;
import com.ticket.system.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("contact")
@Tag(name = "联系人管理", description = "乘客联系人增删改查、默认设置")
public class ContactController {

    @Autowired
    private ContactService contactService;

    @GetMapping("/list/{userId}")
    @Operation(summary = "获取联系人列表", description = "获取指定用户的所有常用联系人")
    public Result<List<Contact>> getContacts(@PathVariable Long userId) {
        List<Contact>  list = contactService.getContacts(userId);
        return Result.success(list);
    }

    @PostMapping("/add")
    @Operation(summary = "添加联系人", description = "新增乘客联系人信息")
    public Result<Boolean> addContact(@RequestBody ContactDTO contact){
        boolean success = contactService.addContact(contact);
        return Result.success("添加成功!",success);
    }

    @DeleteMapping("/delete/{contactId}")
    @Operation(summary = "删除联系人", description = "删除指定联系人ID的联系人")
    public Result<Boolean> deleteContact(@PathVariable  Long contactId){
        boolean success = contactService.deleteContact(contactId);
        return Result.success("删除成功!",success);
    }

    @PutMapping("/default/{contactId}")
    @Operation(summary = "设置默认联系人", description = "将指定联系人设置为默认乘车人")
    public Result<Boolean> setDefault(@PathVariable Long contactId){
        boolean success = contactService.setDefault(contactId);
        return Result.success("设置成功!",success);
    }

    @PostMapping("/cancel/default/{contactId}")
    @Operation(summary = "取消默认联系人", description = "取消指定联系人的默认状态")
    public Result<Boolean> cancelDefault(@PathVariable Long contactId){
        boolean sucess = contactService.cancelDefault(contactId);
        return Result.success("取消成功!",sucess);
    }
}
