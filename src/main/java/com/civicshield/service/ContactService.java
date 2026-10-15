package com.civicshield.service;

import com.civicshield.dto.ContactForm;
import com.civicshield.model.ContactMessage;
import com.civicshield.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persists messages from the public contact form. */
@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactMessageRepository repository;

    @Transactional
    public ContactMessage save(ContactForm form) {
        return repository.save(ContactMessage.builder()
                .name(form.getName().trim())
                .email(form.getEmail().trim())
                .message(form.getMessage().trim())
                .build());
    }
}
