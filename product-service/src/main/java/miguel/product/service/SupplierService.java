package miguel.product.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import miguel.product.dto.SupplierDTO;
import miguel.product.model.Supplier;
import miguel.product.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public List<SupplierDTO> getSuppliers(String query, Boolean activeOnly) {
        List<Supplier> suppliers;
        if (query != null && !query.trim().isEmpty()) {
            suppliers = supplierRepository.searchSuppliers(query.trim());
        } else if (Boolean.TRUE.equals(activeOnly)) {
            suppliers = supplierRepository.findByActiveTrueOrderByCompanyNameAsc();
        } else {
            suppliers = supplierRepository.findAll();
        }

        return suppliers.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SupplierDTO getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado con ID: " + id));
        return toDTO(supplier);
    }

    @Transactional
    public SupplierDTO createSupplier(SupplierDTO dto) {
        if (supplierRepository.existsByNit(dto.getNit())) {
            throw new IllegalArgumentException("Ya existe un proveedor registrado con el NIT: " + dto.getNit());
        }

        Supplier supplier = Supplier.builder()
                .nit(dto.getNit().trim())
                .companyName(dto.getCompanyName().trim())
                .contactName(dto.getContactName() != null ? dto.getContactName().trim() : null)
                .phone(dto.getPhone() != null ? dto.getPhone().trim() : null)
                .email(dto.getEmail() != null ? dto.getEmail().trim() : null)
                .address(dto.getAddress() != null ? dto.getAddress().trim() : null)
                .city(dto.getCity() != null && !dto.getCity().isBlank() ? dto.getCity().trim() : "Cali")
                .paymentTermsDays(dto.getPaymentTermsDays() != null ? dto.getPaymentTermsDays() : 0)
                .active(dto.getActive() != null ? dto.getActive() : true)
                .notes(dto.getNotes() != null ? dto.getNotes().trim() : null)
                .build();

        Supplier saved = supplierRepository.save(supplier);
        return toDTO(saved);
    }

    @Transactional
    public SupplierDTO updateSupplier(Long id, SupplierDTO dto) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado con ID: " + id));

        if (!supplier.getNit().equalsIgnoreCase(dto.getNit()) && supplierRepository.existsByNit(dto.getNit())) {
            throw new IllegalArgumentException("Ya existe otro proveedor registrado con el NIT: " + dto.getNit());
        }

        supplier.setNit(dto.getNit().trim());
        supplier.setCompanyName(dto.getCompanyName().trim());
        supplier.setContactName(dto.getContactName() != null ? dto.getContactName().trim() : null);
        supplier.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        supplier.setEmail(dto.getEmail() != null ? dto.getEmail().trim() : null);
        supplier.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : null);
        if (dto.getCity() != null && !dto.getCity().isBlank()) {
            supplier.setCity(dto.getCity().trim());
        }
        if (dto.getPaymentTermsDays() != null) {
            supplier.setPaymentTermsDays(dto.getPaymentTermsDays());
        }
        if (dto.getActive() != null) {
            supplier.setActive(dto.getActive());
        }
        supplier.setNotes(dto.getNotes() != null ? dto.getNotes().trim() : null);

        Supplier updated = supplierRepository.save(supplier);
        return toDTO(updated);
    }

    @Transactional
    public SupplierDTO toggleSupplierStatus(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado con ID: " + id));

        supplier.setActive(!supplier.getActive());
        Supplier updated = supplierRepository.save(supplier);
        return toDTO(updated);
    }

    public SupplierDTO toDTO(Supplier supplier) {
        return SupplierDTO.builder()
                .id(supplier.getId())
                .nit(supplier.getNit())
                .companyName(supplier.getCompanyName())
                .contactName(supplier.getContactName())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .city(supplier.getCity())
                .paymentTermsDays(supplier.getPaymentTermsDays())
                .active(supplier.getActive())
                .notes(supplier.getNotes())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}
