package com.ticket.system.controller;

import com.ticket.system.common.result.Result;
import com.ticket.system.dto.request.ContactDTO;
import com.ticket.system.entity.Contact;
import com.ticket.system.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("contact")
public class ContactController {

    @Autowired
    private ContactService contactService;

    @GetMapping("/list/{userId}")
    public Result<List<Contact>> getContacts(@PathVariable Long userId) {
        List<Contact>  list = contactService.getContacts(userId);
        return Result.success(list);
    }

    @PostMapping("/add")
    public Result<Boolean> addContact(@RequestBody ContactDTO contact){
        boolean success = contactService.addContact(contact);
        return Result.success("添加成功!",success);
    }

    @DeleteMapping("/delete/{contactId}")
    public Result<Boolean> deleteContact(@PathVariable  Long contactId){
        boolean success = contactService.deleteContact(contactId);
        return Result.success("删除成功!",success);
    }

    @PutMapping("/default/{contactId}")
    public Result<Boolean> setDefault(@PathVariable Long contactId){
        boolean success = contactService.setDefault(contactId);
        return Result.success("设置成功!",success);
    }

    @PostMapping("/cancel/default/{contactId}")
    public Result<Boolean> cancelDefault(@PathVariable Long contactId){
        boolean sucess = contactService.cancelDefault(contactId);
        return Result.success("取消成功!",sucess);
    }
}
