package com.anga9.seller.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.anga9.seller.data.model.SellerOrder
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * GST-compliant invoice generator for B2B orders
 * Generates PDF invoice and shares via Intent
 */
object GstInvoiceGenerator {

    fun generateAndShare(context: Context, order: SellerOrder) {
        try {
            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas
            drawInvoice(canvas, order)
            pdf.finishPage(page)

            // Save to cache
            val fileName = "Invoice_${order.orderId.takeLast(8).uppercase()}.pdf"
            val file = File(context.cacheDir, fileName)
            pdf.writeTo(FileOutputStream(file))
            pdf.close()

            // Share
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "GST Invoice - Order #${order.orderId.takeLast(8).uppercase()}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Invoice"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to generate invoice: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawInvoice(canvas: Canvas, order: SellerOrder) {
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val titlePaint = Paint().apply { textSize = 20f; color = Color.parseColor("#1A365D"); isFakeBoldText = true }
        val headerPaint = Paint().apply { textSize = 14f; color = Color.parseColor("#1A365D"); isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f; color = Color.BLACK }
        val smallPaint = Paint().apply { textSize = 10f; color = Color.GRAY }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        var y = 50f

        // Header
        canvas.drawText("TAX INVOICE", 220f, y, titlePaint)
        y += 30f
        canvas.drawText("ANGA9 Marketplace", 40f, y, headerPaint)
        canvas.drawText("Invoice #: INV-${order.invoiceNumber.ifEmpty { order.orderId.takeLast(8).uppercase() }}", 350f, y, bodyPaint)
        y += 18f
        canvas.drawText("B2B Fresh Marketplace", 40f, y, smallPaint)
        canvas.drawText("Date: ${fmt.format(Date(order.createdAt))}", 350f, y, bodyPaint)
        y += 18f
        canvas.drawText("GSTIN: [SELLER_GSTIN]", 40f, y, smallPaint)
        canvas.drawText("Order #: ${order.orderId.takeLast(8).uppercase()}", 350f, y, bodyPaint)
        y += 25f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        // Bill To
        canvas.drawText("Bill To:", 40f, y, headerPaint)
        y += 18f
        canvas.drawText(order.customerName, 40f, y, bodyPaint)
        y += 15f
        canvas.drawText("Phone: ${order.customerPhone}", 40f, y, bodyPaint)
        if (order.poNumber.isNotEmpty()) {
            y += 15f
            canvas.drawText("PO Number: ${order.poNumber}", 40f, y, bodyPaint)
        }
        y += 25f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        // Items table header
        canvas.drawText("Item", 40f, y, headerPaint)
        canvas.drawText("Qty", 280f, y, headerPaint)
        canvas.drawText("Rate", 340f, y, headerPaint)
        canvas.drawText("GST%", 420f, y, headerPaint)
        canvas.drawText("Amount", 490f, y, headerPaint)
        y += 5f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 18f

        // Items
        order.items.forEach { item ->
            canvas.drawText(item.productName.take(30), 40f, y, bodyPaint)
            canvas.drawText("${item.quantity} ${item.unit}", 280f, y, bodyPaint)
            canvas.drawText("₹${String.format("%.2f", item.unitPrice)}", 340f, y, bodyPaint)
            canvas.drawText("${item.gstPercent}%", 420f, y, bodyPaint)
            canvas.drawText("₹${String.format("%.2f", item.subtotal)}", 490f, y, bodyPaint)
            y += 18f
        }
        y += 5f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        // Totals
        val totalsX = 380f
        canvas.drawText("Subtotal:", totalsX, y, bodyPaint)
        canvas.drawText("₹${String.format("%.2f", order.itemsTotal)}", 490f, y, bodyPaint)
        y += 18f
        if (order.bulkDiscount > 0) {
            canvas.drawText("Bulk Discount:", totalsX, y, bodyPaint)
            canvas.drawText("-₹${String.format("%.2f", order.bulkDiscount)}", 490f, y, bodyPaint)
            y += 18f
        }
        canvas.drawText("GST:", totalsX, y, bodyPaint)
        canvas.drawText("₹${String.format("%.2f", order.gstAmount)}", 490f, y, bodyPaint)
        y += 18f
        canvas.drawText("Delivery:", totalsX, y, bodyPaint)
        canvas.drawText("₹${String.format("%.2f", order.deliveryCharges)}", 490f, y, bodyPaint)
        y += 5f
        canvas.drawLine(totalsX, y, 555f, y, linePaint)
        y += 18f
        canvas.drawText("TOTAL:", totalsX, y, headerPaint)
        canvas.drawText("₹${String.format("%.2f", order.totalAmount)}", 490f, y, headerPaint)
        y += 30f

        // Payment info
        canvas.drawText("Payment Method: ${order.paymentMethod}  |  Status: ${order.paymentStatus}", 40f, y, smallPaint)
        y += 30f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f
        canvas.drawText("This is a computer-generated invoice. No signature required.", 100f, y, smallPaint)
    }
}
