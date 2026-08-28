package com.lms.common.mapper;

import com.lms.common.domain.Address;
import com.lms.common.domain.EmergencyContact;
import com.lms.common.dto.response.AddressResponse;
import com.lms.common.dto.response.EmergencyContactResponse;
import org.springframework.stereotype.Component;

/**
 * Maps the value objects shared by learner and instructor records.
 *
 * <p>Both profiles embed the same {@link Address} and {@link EmergencyContact},
 * so the mapping lives here rather than being duplicated in each module's mapper.
 * A wholly empty embeddable maps to {@code null}: Hibernate materialises one with
 * every field null when all its columns are null, and the API should say "no
 * address on record" rather than return an object of nulls.
 */
@Component
public class ContactMapper {

    public AddressResponse toAddressResponse(Address address) {
        if (address == null || isEmpty(address)) {
            return null;
        }
        AddressResponse resp = new AddressResponse();
        resp.setLine1(address.getLine1());
        resp.setLine2(address.getLine2());
        resp.setCity(address.getCity());
        resp.setState(address.getState());
        resp.setCountry(address.getCountry());
        resp.setPostalCode(address.getPostalCode());
        return resp;
    }

    public EmergencyContactResponse toEmergencyContactResponse(EmergencyContact contact) {
        if (contact == null || isEmpty(contact)) {
            return null;
        }
        EmergencyContactResponse resp = new EmergencyContactResponse();
        resp.setName(contact.getName());
        resp.setRelation(contact.getRelation());
        resp.setPhone(contact.getPhone());
        resp.setEmail(contact.getEmail());
        return resp;
    }

    private boolean isEmpty(Address address) {
        return address.getLine1() == null
                && address.getLine2() == null
                && address.getCity() == null
                && address.getState() == null
                && address.getCountry() == null
                && address.getPostalCode() == null;
    }

    private boolean isEmpty(EmergencyContact contact) {
        return contact.getName() == null
                && contact.getRelation() == null
                && contact.getPhone() == null
                && contact.getEmail() == null;
    }
}
