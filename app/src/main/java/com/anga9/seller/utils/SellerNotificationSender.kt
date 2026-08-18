package com.anga9.seller.utils

/**
 * SELLER NOTIFICATION SENDER - Admin App mein use hoga
 *
 * Yeh file ADMIN APP mein copy karni hai.
 * Admin jab koi action kare toh seller ko FCM notification bhejta hai.
 *
 * Flow:
 * 1. Admin action karta hai (approve product, process payout, etc.)
 * 2. Admin app seller ka fcmToken Firestore se fetch karta hai
 * 3. Firebase Cloud Messaging REST API se notification send karta hai
 *
 * NOTE: Direct FCM send karne ke liye Firebase Admin SDK chahiye
 * jo server-side (Cloud Functions) pe hota hai.
 *
 * PRODUCTION APPROACH:
 * Option A - Firebase Cloud Functions (recommended):
 *   Firestore triggers pe automatically notification send ho
 *
 * Option B - Admin app se direct HTTP call:
 *   FCM Legacy API ya FCM v1 API use karo
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * FIREBASE CLOUD FUNCTIONS - index.js
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Yeh Cloud Functions code Firebase Console mein deploy karna hai:
 *
 * ```javascript
 * const functions = require('firebase-functions');
 * const admin = require('firebase-admin');
 * admin.initializeApp();
 *
 * // Trigger: Jab order create ho
 * exports.notifySellerNewOrder = functions.firestore
 *   .document('orders/{orderId}')
 *   .onCreate(async (snap, context) => {
 *     const order = snap.data();
 *     const sellerId = order.sellerId;
 *     if (!sellerId) return;
 *
 *     const sellerDoc = await admin.firestore()
 *       .collection('sellers').doc(sellerId).get();
 *     const fcmToken = sellerDoc.data()?.fcmToken;
 *     if (!fcmToken) return;
 *
 *     return admin.messaging().send({
 *       token: fcmToken,
 *       notification: {
 *         title: '🛒 New Order Received!',
 *         body: `₹${order.totalAmount} from ${order.customerName}`
 *       },
 *       data: {
 *         type: 'new_order',
 *         orderId: context.params.orderId,
 *         orderAmount: String(order.totalAmount),
 *         customerName: order.customerName || ''
 *       },
 *       android: { priority: 'high' }
 *     });
 *   });
 *
 * // Trigger: Jab product status change ho
 * exports.notifySellerProductStatus = functions.firestore
 *   .document('products/{productId}')
 *   .onUpdate(async (change, context) => {
 *     const before = change.before.data();
 *     const after = change.after.data();
 *     if (before.status === after.status) return; // no change
 *
 *     const sellerId = after.sellerId;
 *     const sellerDoc = await admin.firestore()
 *       .collection('sellers').doc(sellerId).get();
 *     const fcmToken = sellerDoc.data()?.fcmToken;
 *     if (!fcmToken) return;
 *
 *     const isApproved = after.status === 'approved';
 *     return admin.messaging().send({
 *       token: fcmToken,
 *       notification: {
 *         title: isApproved ? '✅ Product Approved!' : '⚠️ Product Rejected',
 *         body: after.name
 *       },
 *       data: {
 *         type: isApproved ? 'product_approved' : 'product_rejected',
 *         productId: context.params.productId,
 *         productName: after.name || '',
 *         reason: after.rejectionReason || ''
 *       }
 *     });
 *   });
 *
 * // Trigger: Jab payout status change ho
 * exports.notifySellerPayoutStatus = functions.firestore
 *   .document('seller_payouts/{payoutId}')
 *   .onUpdate(async (change, context) => {
 *     const before = change.before.data();
 *     const after = change.after.data();
 *     if (before.status === after.status) return;
 *
 *     const sellerId = after.sellerId;
 *     const sellerDoc = await admin.firestore()
 *       .collection('sellers').doc(sellerId).get();
 *     const fcmToken = sellerDoc.data()?.fcmToken;
 *     if (!fcmToken) return;
 *
 *     let title, body, type;
 *     if (after.status === 'approved') {
 *       title = '💰 Payout Approved';
 *       body = `₹${after.amount} approved by admin`;
 *       type = 'payout_approved';
 *     } else if (after.status === 'completed') {
 *       title = '🎉 Payment Received!';
 *       body = `₹${after.amount} transferred | UTR: ${after.utrNumber || 'N/A'}`;
 *       type = 'payout_processed';
 *     } else return;
 *
 *     return admin.messaging().send({
 *       token: fcmToken,
 *       notification: { title, body },
 *       data: {
 *         type,
 *         payoutId: context.params.payoutId,
 *         amount: String(after.amount),
 *         utrNumber: after.utrNumber || ''
 *       }
 *     });
 *   });
 *
 * // Trigger: KYC status change
 * exports.notifySellerKycStatus = functions.firestore
 *   .document('sellers/{sellerId}')
 *   .onUpdate(async (change, context) => {
 *     const before = change.before.data();
 *     const after = change.after.data();
 *     const kycChanged = before.kycStatus !== after.kycStatus
 *       || before.verificationStatus !== after.verificationStatus;
 *     if (!kycChanged) return;
 *
 *     const fcmToken = after.fcmToken;
 *     if (!fcmToken) return;
 *
 *     const isApproved = after.kycStatus === 'approved'
 *       || after.verificationStatus === 'approved';
 *
 *     return admin.messaging().send({
 *       token: fcmToken,
 *       notification: {
 *         title: isApproved ? '🎊 KYC Approved!' : 'KYC Rejected',
 *         body: isApproved
 *           ? 'Your account is now active. Start selling!'
 *           : (after.rejectionReason || 'Please resubmit documents')
 *       },
 *       data: {
 *         type: isApproved ? 'kyc_approved' : 'kyc_rejected',
 *         reason: after.rejectionReason || ''
 *       }
 *     });
 *   });
 * ```
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * DEPLOY COMMANDS:
 * ─────────────────────────────────────────────────────────────────────────────
 * npm install -g firebase-tools
 * firebase login
 * firebase init functions
 * firebase deploy --only functions
 * ─────────────────────────────────────────────────────────────────────────────
 */
object SellerNotificationSender {
    // This is a documentation/reference file
    // Actual sending happens via Firebase Cloud Functions (see above)
    // OR via admin app using FCM HTTP v1 API with service account
}
