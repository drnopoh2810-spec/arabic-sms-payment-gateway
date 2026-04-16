package me.capcom.smsgateway.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.capcom.smsgateway.R
import me.capcom.smsgateway.data.entities.PaymentTransaction
import me.capcom.smsgateway.modules.payment.PaymentService
import me.capcom.smsgateway.modules.payment.PaymentWalletType
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.*

class PaymentFragment : Fragment() {
    
    private val paymentService: PaymentService by inject()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PaymentAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payment, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView(view)
        observePayments()
    }
    
    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewPayments)
        adapter = PaymentAdapter { transaction ->
            // Handle transaction click - show details or confirm
            if (!transaction.isConfirmed) {
                paymentService.confirmPayment(transaction.id)
            }
        }
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }
    
    private fun observePayments() {
        paymentService.getRecentTransactions().observe(viewLifecycleOwner, Observer { transactions ->
            adapter.updateTransactions(transactions)
        })
    }
}

class PaymentAdapter(
    private val onTransactionClick: (PaymentTransaction) -> Unit
) : RecyclerView.Adapter<PaymentViewHolder>() {
    
    private var transactions = listOf<PaymentTransaction>()
    
    fun updateTransactions(newTransactions: List<PaymentTransaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_transaction, parent, false)
        return PaymentViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(transactions[position], onTransactionClick)
    }
    
    override fun getItemCount() = transactions.size
}

class PaymentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    fun bind(transaction: PaymentTransaction, onTransactionClick: (PaymentTransaction) -> Unit) {
        // Bind transaction data to views
        // This would be implemented with actual view binding
        itemView.setOnClickListener {
            onTransactionClick(transaction)
        }
    }
}