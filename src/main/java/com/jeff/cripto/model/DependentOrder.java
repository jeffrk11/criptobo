package com.jeff.cripto.model;

import com.jeff.cripto.utils.annotations.Column;
import com.jeff.cripto.utils.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table("dependent_order")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DependentOrder {
    @Column("id_order")
    private long idOrder;
    @Column("id_dependent")
    private long idDependent;
}
