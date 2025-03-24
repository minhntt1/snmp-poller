package com.home.spring_cpe_stats.vendor;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Optional;

@Table(name = "vendor_dim")
@Entity
@Getter
@Setter
@NoArgsConstructor
public class VendorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer vendorKey;
    private Integer vendorPrefix;
    private String vendorName;

    public static Integer parsePrefix(String vendorPrefix) {
        return Integer.parseInt(
                Optional.ofNullable(vendorPrefix).orElse("0"),
                16);
    }

    public VendorEntity(String vendorPrefix, String vendorName) {
        this.vendorPrefix = parsePrefix(vendorPrefix);
        this.vendorName = vendorName;
    }

    public boolean diffName(VendorEntity other) {
        return !this.vendorName.equals(other.vendorName);
    }
}
