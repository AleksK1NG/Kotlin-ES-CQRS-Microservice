package com.example.microservice.domain

import org.bson.codecs.pojo.annotations.BsonProperty
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal


@Document(collection = "bankAccounts")
open class BankAccountDocument(
    @BsonProperty(value = "aggregateId") var aggregateId: String? = null,
    @BsonProperty(value = "email") var email: String? = null,
    @BsonProperty(value = "balance") var balance: BigDecimal? = null,
    @BsonProperty(value = "currency") var currency: String? = null
) {

    @BsonProperty(value = "_id")
    var id: String? = null

    constructor(aggregateId: String, email: String, balance: BigDecimal) : this() {
        this.aggregateId = aggregateId
        this.email = email
        this.balance = balance
    }


    companion object {
        fun of(bankAccountAggregate: BankAccountAggregate): BankAccountDocument {
            return BankAccountDocument(
                bankAccountAggregate.aggregateId,
                bankAccountAggregate.email,
                bankAccountAggregate.balance,
                bankAccountAggregate.currency
            )
        }
    }


    override fun toString(): String {
        return "BankAccountDocument(id=$id, aggregateId=$aggregateId, email=$email, balance=$balance, currency=$currency)"
    }
}