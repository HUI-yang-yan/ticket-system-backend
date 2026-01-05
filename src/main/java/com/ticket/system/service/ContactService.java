package com.ticket.system.service;

import com.ticket.system.dto.request.ContactDTO;
import com.ticket.system.entity.Contact;

import java.util.List;

public interface ContactService {
    List<Contact> getContacts(Long userId);

    boolean addContact(ContactDTO contact);

    boolean deleteContact(Long contactId);

    boolean setDefault(Long contactId);

    boolean cancelDefault(Long contactId);
}
