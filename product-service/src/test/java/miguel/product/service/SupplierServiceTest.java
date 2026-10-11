package miguel.product.service;

import jakarta.persistence.EntityNotFoundException;
import miguel.product.dto.SupplierDTO;
import miguel.product.model.Supplier;
import miguel.product.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierService supplierService;

    private Supplier testSupplier;

    @BeforeEach
    void setUp() {
        testSupplier = Supplier.builder()
                .id(1L)
                .nit("860005224-6")
                .companyName("Bavaria S.A.")
                .contactName("Juan Perez")
                .phone("3101234567")
                .email("contacto@bavaria.com")
                .city("Cali")
                .paymentTermsDays(30)
                .active(true)
                .build();
    }

    @Test
    void getSuppliers_withQuery_shouldCallSearchSuppliers() {
        when(supplierRepository.searchSuppliers("bavaria")).thenReturn(Collections.singletonList(testSupplier));

        List<SupplierDTO> result = supplierService.getSuppliers("bavaria", null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Bavaria S.A.", result.get(0).getCompanyName());
    }

    @Test
    void createSupplier_withUniqueNit_shouldSaveAndReturnDTO() {
        when(supplierRepository.existsByNit("860005224-6")).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(i -> {
            Supplier s = i.getArgument(0);
            s.setId(10L);
            return s;
        });

        SupplierDTO dto = SupplierDTO.builder()
                .nit("860005224-6")
                .companyName("Bavaria S.A.")
                .paymentTermsDays(30)
                .build();

        SupplierDTO created = supplierService.createSupplier(dto);

        assertNotNull(created);
        assertEquals(10L, created.getId());
        assertEquals("860005224-6", created.getNit());
    }

    @Test
    void createSupplier_withDuplicateNit_shouldThrowIllegalArgumentException() {
        when(supplierRepository.existsByNit("860005224-6")).thenReturn(true);

        SupplierDTO dto = SupplierDTO.builder()
                .nit("860005224-6")
                .companyName("Bavaria S.A.")
                .build();

        assertThrows(IllegalArgumentException.class, () -> supplierService.createSupplier(dto));
    }

    @Test
    void toggleSupplierStatus_shouldInvertActiveFlag() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(i -> i.getArgument(0));

        SupplierDTO toggled = supplierService.toggleSupplierStatus(1L);

        assertNotNull(toggled);
        assertFalse(toggled.getActive());
    }

    @Test
    void getSupplierById_whenNotFound_shouldThrowEntityNotFoundException() {
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> supplierService.getSupplierById(999L));
    }
}
