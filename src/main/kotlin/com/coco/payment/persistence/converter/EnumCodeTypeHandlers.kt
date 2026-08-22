package com.coco.payment.persistence.converter

import com.coco.payment.persistence.enumerator.OrderStatus
import com.coco.payment.persistence.enumerator.PaymentFailCode
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.enumerator.PaymentTransactionStatus
import org.apache.ibatis.type.BaseTypeHandler
import org.apache.ibatis.type.JdbcType
import org.apache.ibatis.type.MappedTypes
import java.sql.CallableStatement
import java.sql.PreparedStatement
import java.sql.ResultSet

abstract class EnumCodeTypeHandler<T>(private val values: Array<T>, private val code: (T) -> Int) : BaseTypeHandler<T>() {
    override fun setNonNullParameter(ps: PreparedStatement, i: Int, parameter: T, jdbcType: JdbcType?) = ps.setInt(i, code(parameter))
    override fun getNullableResult(rs: ResultSet, columnName: String): T? = find(rs.getInt(columnName), rs.wasNull())
    override fun getNullableResult(rs: ResultSet, columnIndex: Int): T? = find(rs.getInt(columnIndex), rs.wasNull())
    override fun getNullableResult(cs: CallableStatement, columnIndex: Int): T? = find(cs.getInt(columnIndex), cs.wasNull())
    private fun find(value: Int, isNull: Boolean) = if (isNull) null else values.first { code(it) == value }
}

@MappedTypes(PaymentSystem::class)
class PaymentSystemTypeHandler : EnumCodeTypeHandler<PaymentSystem>(PaymentSystem.entries.toTypedArray(), PaymentSystem::code)

@MappedTypes(OrderStatus::class)
class OrderStatusTypeHandler : EnumCodeTypeHandler<OrderStatus>(OrderStatus.entries.toTypedArray(), OrderStatus::code)

@MappedTypes(PaymentTransactionStatus::class)
class PaymentTransactionStatusTypeHandler : EnumCodeTypeHandler<PaymentTransactionStatus>(PaymentTransactionStatus.entries.toTypedArray(), PaymentTransactionStatus::code)

@MappedTypes(PaymentFailCode::class)
class PaymentFailCodeTypeHandler : EnumCodeTypeHandler<PaymentFailCode>(PaymentFailCode.entries.toTypedArray(), PaymentFailCode::code)
