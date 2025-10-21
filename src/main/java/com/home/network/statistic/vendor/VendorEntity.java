package com.home.network.statistic.vendor;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public void updateName(VendorEntity other) {
        if (!this.vendorName.equals(other.vendorName))
            this.vendorName = other.vendorName;
    }

    public static Map<Integer, VendorEntity> toMap(List<VendorEntity> list) {
        return list.stream().collect(Collectors.toMap(VendorEntity::getVendorPrefix, Function.identity()));
    }
}
