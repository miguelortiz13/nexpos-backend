package miguel.sales.service;

import lombok.RequiredArgsConstructor;
import miguel.sales.dto.CustomerDTO;
import miguel.sales.model.Customer;
import miguel.sales.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));
    }

    public Optional<Customer> getCustomerByDoc(String docNumber) {
        return customerRepository.findByDocNumber(docNumber);
    }

    public List<Customer> searchCustomers(String query) {
        if (query == null || query.trim().isBlank()) {
            return customerRepository.findAll();
        }
        return customerRepository.searchCustomers(query.trim());
    }

    @Transactional
    public Customer createCustomer(CustomerDTO dto) {
        if (customerRepository.existsByDocNumber(dto.getDocNumber())) {
            throw new RuntimeException("Ya existe un cliente registrado con el documento: " + dto.getDocNumber());
        }

        Customer customer = Customer.builder()
                .docType(dto.getDocType() != null && !dto.getDocType().isBlank() ? dto.getDocType() : "CC")
                .docNumber(dto.getDocNumber().trim())
                .name(dto.getName().trim())
                .email(dto.getEmail() != null ? dto.getEmail().trim() : null)
                .phone(dto.getPhone() != null ? dto.getPhone().trim() : null)
                .address(dto.getAddress() != null ? dto.getAddress().trim() : null)
                .city(dto.getCity() != null ? dto.getCity().trim() : "Cali")
                .department(dto.getDepartment() != null ? dto.getDepartment().trim() : "Valle del Cauca")
                .notes(dto.getNotes())
                .creditAllowed(dto.getCreditAllowed() != null ? dto.getCreditAllowed() : false)
                .creditLimit(dto.getCreditLimit() != null && dto.getCreditLimit().compareTo(java.math.BigDecimal.ZERO) >= 0 ? dto.getCreditLimit() : java.math.BigDecimal.ZERO)
                .currentDebt(java.math.BigDecimal.ZERO)
                .build();

        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(Long id, CustomerDTO dto) {
        Customer customer = getCustomerById(id);

        if (!customer.getDocNumber().equalsIgnoreCase(dto.getDocNumber().trim()) &&
                customerRepository.existsByDocNumber(dto.getDocNumber().trim())) {
            throw new RuntimeException("El nuevo número de documento ya pertenece a otro cliente");
        }

        if (dto.getDocType() != null && !dto.getDocType().isBlank()) {
            customer.setDocType(dto.getDocType());
        }
        customer.setDocNumber(dto.getDocNumber().trim());
        customer.setName(dto.getName().trim());
        customer.setEmail(dto.getEmail() != null ? dto.getEmail().trim() : null);
        customer.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        customer.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : null);
        if (dto.getCity() != null) customer.setCity(dto.getCity().trim());
        if (dto.getDepartment() != null) customer.setDepartment(dto.getDepartment().trim());
        if (dto.getNotes() != null) customer.setNotes(dto.getNotes());
        if (dto.getCreditAllowed() != null) customer.setCreditAllowed(dto.getCreditAllowed());
        if (dto.getCreditLimit() != null && dto.getCreditLimit().compareTo(java.math.BigDecimal.ZERO) >= 0) {
            customer.setCreditLimit(dto.getCreditLimit());
        }

        return customerRepository.save(customer);
    }

    @Transactional
    public Customer getOrCreateCustomer(String docNumber, String name, String email, String phone) {
        if (docNumber == null || docNumber.isBlank() || "222222222222".equals(docNumber)) {
            return customerRepository.findByDocNumber("222222222222")
                    .orElseGet(() -> customerRepository.save(Customer.builder()
                            .docType("CC")
                            .docNumber("222222222222")
                            .name("Consumidor Final")
                            .email("facturacion@nexpos.com.co")
                            .build()));
        }

        return customerRepository.findByDocNumber(docNumber)
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .docType("CC")
                        .docNumber(docNumber.trim())
                        .name(name != null && !name.isBlank() ? name.trim() : "Cliente " + docNumber)
                        .email(email != null && !email.isBlank() ? email.trim() : null)
                        .phone(phone != null && !phone.isBlank() ? phone.trim() : null)
                        .city("Cali")
                        .department("Valle del Cauca")
                        .build()));
    }
}
