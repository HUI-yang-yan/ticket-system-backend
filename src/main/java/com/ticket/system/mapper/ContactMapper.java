package com.ticket.system.mapper;

import com.ticket.system.entity.Contact;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ContactMapper {
    List<Contact> selectByUserId(Long userId);

    int insert(Contact contact);

    void deleteByUserIdAndContactId(Long userId, Long contactId);

    Contact selectByUserIdAndContactId(Long userId, Long contactId);

    Contact selectDefault(Long userId);

    void update(Contact defaultContact);
}
