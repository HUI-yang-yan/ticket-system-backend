package com.ticket.system.service.impl;

import com.ticket.system.common.exception.BusinessException;
import com.ticket.system.common.exception.ErrorCode;
import com.ticket.system.common.util.ThreadLocalUtil;
import com.ticket.system.dto.request.ContactDTO;
import com.ticket.system.entity.Contact;
import com.ticket.system.mapper.ContactMapper;
import com.ticket.system.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactServiceImpl implements ContactService {
    @Autowired
    private ContactMapper contactMapper;

    @Override
    public List<Contact> getContacts(Long userId) {
        return contactMapper.selectByUserId(userId);
    }

    @Override
    public boolean addContact(ContactDTO contactDTO) {
        Contact contact = getContact(contactDTO);
        int sum = contactMapper.insert(contact);
        if(sum>0){
            return true;
        }
        throw new BusinessException(ErrorCode.CONTACT_ADD_ERROR.getCode(),"添加失败!");
    }

    private static Contact getContact(ContactDTO contactDTO) {
        Contact contact = new Contact();
        contact.setUserId(ThreadLocalUtil.getUserId());
        contact.setName(contactDTO.getRealName());
        contact.setPhone(contactDTO.getPhone());
        contact.setIdCard(contactDTO.getIdCard());
        contact.setType(contactDTO.getType());
        return contact;
    }

    @Override
    public boolean deleteContact(Long contactId) {
        Long userId = ThreadLocalUtil.getUserId();
        try {
            contactMapper.deleteByUserIdAndContactId(userId,contactId);
            return false;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CONTACT_DELETE_ERROR.getCode(),"删除失败!");
        }
    }

    @Override
    public boolean setDefault(Long contactId) {
        Long userId = ThreadLocalUtil.getUserId();
        Contact contact = contactMapper.selectByUserIdAndContactId(userId,contactId);
        if(contact==null){
            throw new BusinessException(ErrorCode.CONTACT_USER_NULL_ERROR.getCode(),"用户不存在!");
        }
        contact.setIsDefault(1);
        Contact defaultContact = contactMapper.selectDefault(userId);
        if(defaultContact!=null){
            defaultContact.setIsDefault(0);
            contactMapper.update(defaultContact);
        }
        contactMapper.update(contact);
        return true;
    }

    @Override
    public boolean cancelDefault(Long contactId) {
        Long userId = ThreadLocalUtil.getUserId();
        Contact defaultContact = contactMapper.selectByUserIdAndContactId(userId,contactId);
        if(defaultContact==null){
            return false;
        }
        defaultContact.setIsDefault(0);
        contactMapper.update(defaultContact);
        return true;
    }
}
