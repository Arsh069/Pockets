import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Edit2,
  Trash2,
  RotateCcw,
  Sliders,
  QrCode,
  Send,
  Lock,
  ExternalLink,
  CheckCircle2,
  XCircle,
  Clock,
  AlertTriangle,
  X,
  Camera,
  RefreshCw,
} from 'lucide-react';
import jsQR from 'jsqr';
import { api } from '../api';
import { useToast } from '../context/ToastContext';

// Real Brand Vector Logos & Typography
const GPayLogo = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ flexShrink: 0 }}>
    <path d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17z" fill="#4285F4"/>
    <path d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.26v3.15C3.27 21.36 7.34 24 12 24z" fill="#34A853"/>
    <path d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.26C.46 8.16 0 9.94 0 12s.46 3.84 1.26 5.42l4.02-3.15z" fill="#FBBC05"/>
    <path d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.34 0 3.27 2.64 1.26 6.58l4.02 3.15c.95-2.83 3.6-4.98 6.72-4.98z" fill="#EA4335"/>
  </svg>
);

const PhonePeLogo = () => (
  <svg
    width="18"
    height="18"
    fill="#5F259F"
    role="img"
    viewBox="0 0 24 24"
    style={{ flexShrink: 0 }}
  >
    <title>PhonePe</title>
    <path d="M10.206 9.941h2.949v4.692c-.402.201-.938.268-1.34.268-1.072 0-1.609-.536-1.609-1.743V9.941zm13.47 4.816c-1.523 6.449-7.985 10.442-14.433 8.919C2.794 22.154-1.199 15.691.324 9.243 1.847 2.794 8.309-1.199 14.757.324c6.449 1.523 10.442 7.985 8.919 14.433zm-6.231-5.888a.887.887 0 0 0-.871-.871h-1.609l-3.686-4.222c-.335-.402-.871-.536-1.407-.402l-1.274.401c-.201.067-.268.335-.134.469l4.021 3.82H6.386c-.201 0-.335.134-.335.335v.67c0 .469.402.871.871.871h.938v3.217c0 2.413 1.273 3.82 3.418 3.82.67 0 1.206-.067 1.877-.335v2.145c0 .603.469 1.072 1.072 1.072h.938a.432.432 0 0 0 .402-.402V9.874h1.542c.201 0 .335-.134.335-.335v-.67z" />
  </svg>
);

const PaytmLogo = () => (
  <svg
    width="18"
    height="18"
    fill="#20336B"
    role="img"
    viewBox="0 0 24 24"
    style={{ flexShrink: 0 }}
  >
    <title>Paytm</title>
    <path d="M15.85 8.167a.204.204 0 0 0-.04.004c-.68.19-.543 1.148-1.781 1.23h-.12a.23.23 0 0 0-.052.005h-.001a.24.24 0 0 0-.184.235v1.09c0 .134.106.241.237.241h.645v4.623c0 .132.104.238.233.238h1.058a.236.236 0 0 0 .233-.238v-4.623h.6c.13 0 .236-.107.236-.241v-1.09a.239.239 0 0 0-.236-.24h-.612V8.386a.218.218 0 0 0-.216-.22zm4.225 1.17c-.398 0-.762.15-1.042.395v-.124a.238.238 0 0 0-.234-.224h-1.07a.24.24 0 0 0-.236.242v5.92a.24.24 0 0 0 .236.242h1.07c.12 0 .217-.091.233-.209v-4.25a.393.393 0 0 1 .371-.408h.196a.41.41 0 0 1 .226.09.405.405 0 0 1 .145.319v4.074l.004.155a.24.24 0 0 0 .237.241h1.07a.239.239 0 0 0 .235-.23l-.001-4.246c0-.14.062-.266.174-.34a.419.419 0 0 1 .196-.068h.198c.23.02.37.2.37.408.005 1.396.004 2.8.004 4.224a.24.24 0 0 0 .237.241h1.07c.13 0 .236-.108.236-.241v-4.543c0-.31-.034-.442-.08-.577a1.601 1.601 0 0 0-1.51-1.09h-.015a1.58 1.58 0 0 0-1.152.5c-.291-.308-.7-.5-1.153-.5zM.232 9.4A.234.234 0 0 0 0 9.636v5.924c0 .132.096.238.216.241h1.09c.13 0 .237-.107.237-.24l.004-1.658H2.57c.857 0 1.453-.605 1.453-1.481v-1.538c0-.877-.596-1.484-1.453-1.484H.232zm9.032 0a.239.239 0 0 0-.237.241v2.47c0 .94.657 1.608 1.579 1.608h.675s.016 0 .037.004a.253.253 0 0 1 .222.253c0 .13-.096.235-.219.251l-.018.004-.303.006H9.739a.239.239 0 0 0-.236.24v1.09a.24.24 0 0 0 .236.242h1.75c.92 0 1.577-.669 1.577-1.608v-4.56a.239.239 0 0 0-.236-.24h-1.07a.239.239 0 0 0-.236.24c-.005.787 0 1.525 0 2.255a.253.253 0 0 1-.25.25h-.449a.253.253 0 0 1-.25-.255c.005-.754-.005-1.5-.005-2.25a.239.239 0 0 0-.236-.24zm-4.004.006a.232.232 0 0 0-.238.226v1.023c0 .132.113.24.252.24h1.413c.112.017.2.1.213.23v.14c-.013.124-.1.214-.207.224h-.7c-.93 0-1.594.63-1.594 1.515v1.269c0 .88.57 1.506 1.495 1.506h1.94c.348 0 .63-.27.63-.6v-4.136c0-1.004-.508-1.637-1.72-1.637zm-3.713 1.572h.678c.139 0 .25.115.25.256v.836a.253.253 0 0 1-.25.256h-.1c-.192.002-.386 0-.578 0zm4.67 1.977h.445c.139 0 .252.108.252.24v.932a.23.23 0 0 1-.014.076.25.25 0 0 1-.238.164h-.445a.247.247 0 0 1-.252-.24v-.933c0-.132.113-.239.252-.239Z" />
  </svg>
);

export const PocketDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [pocket, setPocket] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  // Edit Pocket Modal
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editName, setEditName] = useState('');
  const [editLimit, setEditLimit] = useState('');
  const [savingEdit, setSavingEdit] = useState(false);

  // Correct Balance Modal
  const [isOverrideModalOpen, setIsOverrideModalOpen] = useState(false);
  const [overrideBalanceInput, setOverrideBalanceInput] = useState('');
  const [savingOverride, setSavingOverride] = useState(false);

  // Delete Confirm Modal
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  // Pay Mode Tab ('manual' | 'qr')
  const [payMode, setPayMode] = useState('manual');
  const [payeeUpiId, setPayeeUpiId] = useState('');
  const [payAmount, setPayAmount] = useState('');
  const [payNote, setPayNote] = useState('');
  const [submittingPay, setSubmittingPay] = useState(false);

  // QR Scanning & Preview State (Local Non-Authoritative Parse)
  const [cameraActive, setCameraActive] = useState(false);
  const [cameraError, setCameraError] = useState(null);
  const [qrPreview, setQrPreview] = useState(null);
  const [qrAmountInput, setQrAmountInput] = useState('');
  const [qrNoteInput, setQrNoteInput] = useState('');

  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const animationFrameRef = useRef(null);
  const mediaStreamRef = useRef(null);
  const isMountedRef = useRef(true);

  // Active Pending Transaction Flow
  const [activeTx, setActiveTx] = useState(null);
  const [generatingLink, setGeneratingLink] = useState(false);
  const [confirmingTxId, setConfirmingTxId] = useState(null);
  const [cancellingTxId, setCancellingTxId] = useState(null);
  const [timeRemaining, setTimeRemaining] = useState('');

  // Log Manual Purchase Form
  const [logAmount, setLogAmount] = useState('');
  const [logNote, setLogNote] = useState('');
  const [submittingLog, setSubmittingLog] = useState(false);

  // Check if any transaction in this pocket is PENDING
  const hasPendingTransaction = transactions.some((t) => t.status === 'PENDING');

  // Fetch Pocket & Transactions
  const loadData = async () => {
    try {
      const [pocketData, txList] = await Promise.all([
        api.pockets.get(id),
        api.transactions.listByPocket(id),
      ]);

      // Check client-side expired transactions and update display
      const now = Date.now();
      const processedTxList = (txList || []).map((tx) => {
        if (tx.status === 'PENDING' && tx.expiresAt && new Date(tx.expiresAt).getTime() <= now) {
          return { ...tx, status: 'EXPIRED' };
        }
        return tx;
      });

      setPocket(pocketData);
      setTransactions(processedTxList);

      // Refresh active pending transaction if present
      if (activeTx) {
        const matchingTx = processedTxList.find((t) => t.id === activeTx.id);
        if (matchingTx && matchingTx.status !== 'PENDING') {
          setActiveTx(null);
        } else if (matchingTx) {
          setActiveTx(matchingTx);
        }
      }
    } catch (err) {
      showToast(err.message || 'Failed to load pocket details');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    isMountedRef.current = true;
    loadData();

    return () => {
      isMountedRef.current = false;
      stopCameraStream();
    };
  }, [id]);

  // Expiry Countdown Timer for Active Pending Transaction
  useEffect(() => {
    if (!activeTx || !activeTx.expiresAt) return;

    const interval = setInterval(() => {
      const expiry = new Date(activeTx.expiresAt).getTime();
      const now = new Date().getTime();
      const diff = expiry - now;

      if (diff <= 0) {
        setTimeRemaining('Expired');
        setActiveTx((prev) => (prev ? { ...prev, status: 'EXPIRED' } : null));
        setTransactions((prev) =>
          prev.map((t) => (t.id === activeTx.id ? { ...t, status: 'EXPIRED' } : t))
        );
        clearInterval(interval);
      } else {
        const minutes = Math.floor(diff / 60000);
        const seconds = Math.floor((diff % 60000) / 1000);
        setTimeRemaining(`${minutes}m ${seconds < 10 ? '0' : ''}${seconds}s`);
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [activeTx]);

  // ----------------------------------------------------
  // CAMERA LIFECYCLE CONTROLLER (Fix #4: Explicit teardown & re-init)
  // ----------------------------------------------------
  const stopCameraStream = () => {
    if (animationFrameRef.current) {
      cancelAnimationFrame(animationFrameRef.current);
      animationFrameRef.current = null;
    }
    if (mediaStreamRef.current) {
      mediaStreamRef.current.getTracks().forEach((track) => {
        try {
          track.stop();
        } catch (e) {}
      });
      mediaStreamRef.current = null;
    }
    if (videoRef.current) {
      if (videoRef.current.srcObject) {
        const stream = videoRef.current.srcObject;
        if (stream && stream.getTracks) {
          stream.getTracks().forEach((track) => {
            try {
              track.stop();
            } catch (e) {}
          });
        }
        videoRef.current.srcObject = null;
      }
    }
    setCameraActive(false);
  };

  const startCameraStream = async () => {
    // Explicitly tear down any existing stream before requesting a new one
    stopCameraStream();
    setCameraError(null);
    setCameraActive(true);

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' },
      });
      if (!isMountedRef.current) {
        stream.getTracks().forEach((t) => t.stop());
        return;
      }
      mediaStreamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        videoRef.current.setAttribute('playsinline', 'true');
        await videoRef.current.play().catch(() => {});
        animationFrameRef.current = requestAnimationFrame(scanTick);
      }
    } catch (err) {
      if (!isMountedRef.current) return;
      stopCameraStream();
      setCameraError('Camera access denied or unavailable on this device.');
    }
  };

  const scanTick = () => {
    if (videoRef.current && videoRef.current.readyState === videoRef.current.HAVE_ENOUGH_DATA) {
      const canvas = canvasRef.current;
      if (canvas) {
        const ctx = canvas.getContext('2d');
        canvas.width = videoRef.current.videoWidth;
        canvas.height = videoRef.current.videoHeight;
        ctx.drawImage(videoRef.current, 0, 0, canvas.width, canvas.height);
        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        const code = jsQR(imageData.data, imageData.width, imageData.height, {
          inversionAttempts: 'dontInvert',
        });

        if (code && code.data) {
          // Immediately stop camera once decoded
          stopCameraStream();
          handleQrDecoded(code.data);
          return;
        }
      }
    }
    if (mediaStreamRef.current) {
      animationFrameRef.current = requestAnimationFrame(scanTick);
    }
  };

  // Switch tabs
  useEffect(() => {
    if (payMode === 'qr' && !qrPreview && !activeTx) {
      startCameraStream();
    } else {
      stopCameraStream();
    }
  }, [payMode]);

  // ----------------------------------------------------
  // QR DECODE & PREVIEW FLOW (Fix #3: Local non-authoritative parse & explicit Pay commit)
  // ----------------------------------------------------
  const handleQrDecoded = (rawPayload) => {
    let parsedVpa = '';
    let parsedName = null;
    let parsedAmount = null;

    try {
      if (rawPayload.startsWith('upi://pay')) {
        const url = new URL(rawPayload);
        parsedVpa = url.searchParams.get('pa') || '';
        parsedName = url.searchParams.get('pn') || null;
        const am = url.searchParams.get('am');
        if (am && parseFloat(am) > 0) {
          parsedAmount = parseFloat(am);
        }
      }
    } catch (e) {
      const paMatch = rawPayload.match(/[?&]pa=([^&]+)/i);
      if (paMatch) parsedVpa = decodeURIComponent(paMatch[1]);
      const pnMatch = rawPayload.match(/[?&]pn=([^&]+)/i);
      if (pnMatch) parsedName = decodeURIComponent(pnMatch[1]);
      const amMatch = rawPayload.match(/[?&]am=([0-9.]+)/i);
      if (amMatch && parseFloat(amMatch[1]) > 0) parsedAmount = parseFloat(amMatch[1]);
    }

    setQrPreview({
      rawPayload,
      payeeUpiId: parsedVpa || 'Merchant UPI',
      payeeName: parsedName ? parsedName.replace(/\+/g, ' ') : null,
      embeddedAmount: parsedAmount,
      isAmountLocked: parsedAmount !== null,
    });
    setQrAmountInput(parsedAmount ? parsedAmount.toString() : '');
    setQrNoteInput('');
  };

  // Step d: Cancel preview and discard scan without touching backend
  const handleCancelPreview = () => {
    setQrPreview(null);
    setQrAmountInput('');
    setQrNoteInput('');
    // Cleanly re-start camera for new scan
    startCameraStream();
  };

  // Step b & c: User taps explicit "Pay" button -> call backend POST /api/transactions then generate-link
  const handleConfirmAndPayQr = async (e) => {
    e.preventDefault();
    if (!qrPreview) return;

    if (hasPendingTransaction) {
      showToast('This pocket has a pending payment — confirm or cancel it first');
      return;
    }

    const finalAmount = qrPreview.isAmountLocked
      ? qrPreview.embeddedAmount
      : parseFloat(qrAmountInput);

    if (!finalAmount || isNaN(finalAmount) || finalAmount <= 0) {
      showToast('Please enter a valid amount greater than 0');
      return;
    }

    setSubmittingPay(true);
    try {
      // 1. Create transaction with rawQrPayload (authoritative server validation & 10-minute expiry clock begins now)
      const payload = {
        pocketId: parseInt(id, 10),
        rawQrPayload: qrPreview.rawPayload,
        amount: finalAmount,
        idempotencyKey: crypto.randomUUID ? crypto.randomUUID() : `qr_${Date.now()}_${Math.random()}`,
        note: qrNoteInput.trim() || null,
      };

      const tx = await api.transactions.create(payload);
      setActiveTx(tx);
      setQrPreview(null);
      setQrAmountInput('');
      setQrNoteInput('');

      // 2. Generate UPI deep-link
      const linkRes = await api.payments.generateLink(tx.id);

      // 3. Show handoff message and trigger deep-link navigation
      showToast('Opening your UPI app...', 'success');

      if (linkRes && linkRes.upiDeepLink) {
        window.location.href = linkRes.upiDeepLink;
      }

      await loadData();
    } catch (err) {
      showToast(err.message || 'Failed to process QR payment');
    } finally {
      setSubmittingPay(false);
    }
  };

  // Action: Edit Pocket
  const handleEditPocket = async (e) => {
    e.preventDefault();
    if (hasPendingTransaction) {
      showToast('This pocket has a pending payment — confirm or cancel it first');
      return;
    }
    const limit = parseFloat(editLimit);
    if (isNaN(limit) || limit <= 0) {
      showToast('Monthly limit must be greater than 0');
      return;
    }

    setSavingEdit(true);
    try {
      const updated = await api.pockets.update(id, editName.trim(), limit);
      setPocket(updated);
      setIsEditModalOpen(false);
      showToast('Pocket updated successfully!', 'success');
    } catch (err) {
      showToast(err.message || 'Failed to update pocket');
    } finally {
      setSavingEdit(false);
    }
  };

  // Action: Delete Pocket (Fix #1a: Blocked with 409 if pending)
  const handleDeletePocket = async () => {
    if (hasPendingTransaction) {
      showToast('This pocket has a pending payment — confirm or cancel it first');
      return;
    }
    setDeleting(true);
    try {
      await api.pockets.delete(id);
      showToast('Pocket deleted successfully', 'success');
      navigate('/dashboard');
    } catch (err) {
      showToast(err.message || 'Failed to delete pocket');
      setDeleting(false);
    }
  };

  // Action: Reset this pocket
  const handleResetPocket = async () => {
    if (hasPendingTransaction) {
      showToast('This pocket has a pending payment — confirm or cancel it first');
      return;
    }
    try {
      const updated = await api.pockets.reset(id);
      setPocket(updated);
      showToast('Pocket reset to monthly limit!', 'success');
      await loadData();
    } catch (err) {
      showToast(err.message || 'Failed to reset pocket');
    }
  };

  // Action: Correct Balance (PATCH)
  const handleOverrideBalance = async (e) => {
    e.preventDefault();
    if (hasPendingTransaction) {
      showToast('This pocket has a pending payment — confirm or cancel it first');
      return;
    }
    const val = parseFloat(overrideBalanceInput);
    if (isNaN(val) || val < 0 || val > Number(pocket.monthlyLimit)) {
      showToast(`Balance must be between 0 and the monthly limit (${formatCurrency(pocket.monthlyLimit)})`);
      return;
    }

    setSavingOverride(true);
    try {
      const updated = await api.pockets.overrideBalance(id, val);
      setPocket(updated);
      setIsOverrideModalOpen(false);
      showToast('Balance corrected successfully!', 'success');
      await loadData();
    } catch (err) {
      showToast(err.message || 'Failed to correct balance');
    } finally {
      setSavingOverride(false);
    }
  };

  // Action: Create In-App Transaction for Manual UPI Mode
  const handleCreateManualPayment = async (e) => {
    e.preventDefault();
    if (hasPendingTransaction) {
      showToast('This pocket has a pending payment — confirm or cancel it first');
      return;
    }

    const upiPattern = /^[\w.\-]{2,256}@[a-zA-Z]{2,64}$/;
    if (!payeeUpiId.trim() || !upiPattern.test(payeeUpiId.trim())) {
      showToast('Please enter a valid UPI ID (e.g. name@okhdfcbank or 9876543210@paytm)');
      return;
    }
    if (!payAmount || parseFloat(payAmount) <= 0) {
      showToast('Amount must be greater than 0');
      return;
    }

    setSubmittingPay(true);
    try {
      const payload = {
        pocketId: parseInt(id, 10),
        payeeUpiId: payeeUpiId.trim(),
        amount: parseFloat(payAmount),
        note: payNote.trim() || null,
        idempotencyKey: crypto.randomUUID ? crypto.randomUUID() : `man_${Date.now()}_${Math.random()}`,
      };

      const tx = await api.transactions.create(payload);
      setActiveTx(tx);
      setPayeeUpiId('');
      setPayAmount('');
      setPayNote('');
      showToast('Payment initiated! Confirm or pay via UPI.', 'success');
      await loadData();
    } catch (err) {
      showToast(err.message || 'Failed to create payment');
    } finally {
      setSubmittingPay(false);
    }
  };

  const isIOS =
    typeof navigator !== 'undefined' &&
    (/iPad|iPhone|iPod/.test(navigator.userAgent || '') ||
      (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1));

  // Action: Generate Generic UPI Deep-Link
  const handlePayViaUpi = async (txId) => {
    const targetId = txId || activeTx?.id;
    if (!targetId) return;
    setGeneratingLink(true);
    try {
      const res = await api.payments.generateLink(targetId);
      showToast('Opening your UPI app...', 'success');
      if (res && res.upiDeepLink) {
        window.location.href = res.upiDeepLink;
      }
    } catch (err) {
      showToast(err.message || 'Failed to generate UPI payment link');
    } finally {
      setGeneratingLink(false);
    }
  };

  // Action: Generate Specific App Deep-Link (Google Pay / PhonePe / Paytm)
  const handlePayViaSpecificApp = async (appKey, txId) => {
    const targetId = txId || activeTx?.id;
    if (!targetId) return;
    setGeneratingLink(true);
    try {
      const res = await api.payments.generateLink(targetId);
      showToast('Opening your UPI app...', 'success');
      const specificLink = res?.iosAppLinks?.[appKey] || res?.upiDeepLink;
      if (specificLink) {
        window.location.href = specificLink;
      }
    } catch (err) {
      showToast(err.message || 'Failed to generate specific app payment link');
    } finally {
      setGeneratingLink(false);
    }
  };

  // Action: Confirm Payment (from active card or inline row)
  const handleConfirmPayment = async (txId) => {
    setConfirmingTxId(txId);
    try {
      await api.transactions.confirm(txId);
      showToast('Payment confirmed and balance deducted!', 'success');
      if (activeTx?.id === txId) setActiveTx(null);
      await loadData();
    } catch (err) {
      if (err.status === 409 && err.message?.toLowerCase().includes('expired')) {
        setTransactions((prev) =>
          prev.map((t) => (t.id === txId ? { ...t, status: 'EXPIRED' } : t))
        );
        if (activeTx?.id === txId) setActiveTx(null);
        showToast('This payment request has expired. Please create a new one.');
      } else {
        showToast(err.message || 'Payment confirmation failed');
      }
      await loadData();
    } finally {
      setConfirmingTxId(null);
    }
  };

  // Action: Cancel Payment (from active card or inline row)
  const handleCancelPayment = async (txId) => {
    setCancellingTxId(txId);
    try {
      await api.transactions.cancel(txId);
      showToast('Payment cancelled', 'success');
      if (activeTx?.id === txId) setActiveTx(null);
      await loadData();
    } catch (err) {
      showToast(err.message || 'Failed to cancel payment');
      await loadData();
    } finally {
      setCancellingTxId(null);
    }
  };

  // Action: Log Manual Purchase
  const handleLogPurchase = async (e) => {
    e.preventDefault();
    if (hasPendingTransaction) {
      showToast('This pocket has a pending payment — confirm or cancel it first');
      return;
    }
    const amt = parseFloat(logAmount);
    if (isNaN(amt) || amt <= 0) {
      showToast('Amount must be greater than 0');
      return;
    }
    setSubmittingLog(true);
    try {
      await api.transactions.log({
        pocketId: parseInt(id, 10),
        amount: amt,
        note: logNote.trim() || null,
      });
      setLogAmount('');
      setLogNote('');
      showToast('External purchase logged successfully!', 'success');
      await loadData();
    } catch (err) {
      showToast(err.message || 'Failed to log purchase');
    } finally {
      setSubmittingLog(false);
    }
  };

  // Currency Formatter
  const formatCurrency = (val) => {
    const num = Math.max(0, Number(val || 0));
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2,
    }).format(num);
  };

  if (loading || !pocket) {
    return (
      <div style={{ padding: '60px 0', textAlign: 'center', color: 'var(--color-text-secondary)' }}>
        <div style={{ fontSize: '1.1rem', fontWeight: 600 }}>Loading pocket details...</div>
      </div>
    );
  }

  const overspentVal = Number(pocket.overspentAmount || 0);

  return (
    <div>
      {/* Back Button */}
      <div style={{ marginBottom: '20px' }}>
        <Link to="/dashboard" className="btn btn-ghost btn-sm" style={{ paddingLeft: 0 }}>
          <ArrowLeft size={18} />
          <span>All Pockets</span>
        </Link>
      </div>

      {/* Proactive Pending Payment Lock Banner */}
      {hasPendingTransaction && (
        <div className="pending-lock-alert">
          <Lock size={18} />
          <div>
            <strong>Pocket Locked:</strong> This pocket has a pending payment. Please confirm or cancel it below before making changes, resetting, or logging purchases.
          </div>
        </div>
      )}

      {/* SECTION 1: HEADER */}
      <div className="card" style={{ marginBottom: '24px' }}>
        <div className="pocket-header-main">
          <div>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 800, letterSpacing: '-0.03em' }}>{pocket.name}</h1>

            {/* Big prominent balance */}
            <div className="tile-balance" style={{ margin: '8px 0 4px' }}>
              {formatCurrency(pocket.balance)}
            </div>

            <div style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--color-text-secondary)' }}>
              of {formatCurrency(pocket.monthlyLimit)} monthly limit
            </div>

            {/* Overspent Warning Element */}
            {overspentVal > 0 && (
              <div className="overspent-alert">
                <AlertTriangle size={18} />
                <span>You've overspent by {formatCurrency(overspentVal)} this cycle</span>
              </div>
            )}
          </div>

          <div className="pocket-header-actions">
            <button
              onClick={() => {
                setEditName(pocket.name);
                setEditLimit(pocket.monthlyLimit);
                setIsEditModalOpen(true);
              }}
              className="btn btn-secondary btn-sm"
              disabled={hasPendingTransaction}
              title={hasPendingTransaction ? 'Pocket is locked while payment is pending' : 'Edit pocket'}
            >
              <Edit2 size={16} />
              <span>Edit</span>
            </button>
            <button
              onClick={() => setIsDeleteModalOpen(true)}
              className="btn btn-ghost btn-sm"
              style={{ color: 'var(--color-danger)' }}
              disabled={hasPendingTransaction}
              title={hasPendingTransaction ? 'Pocket is locked while payment is pending' : 'Delete pocket'}
            >
              <Trash2 size={16} />
            </button>
          </div>
        </div>

        <div className="pocket-tools-row">
          <button
            onClick={handleResetPocket}
            className="btn btn-secondary btn-sm"
            disabled={hasPendingTransaction}
            title={hasPendingTransaction ? 'Pocket is locked while payment is pending' : 'Reset pocket to monthly limit'}
          >
            <RotateCcw size={15} />
            <span>Reset this pocket</span>
          </button>
          <button
            onClick={() => {
              setOverrideBalanceInput(pocket.balance || '0');
              setIsOverrideModalOpen(true);
            }}
            className="btn btn-secondary btn-sm"
            disabled={hasPendingTransaction}
            title={hasPendingTransaction ? 'Pocket is locked while payment is pending' : 'Correct pocket balance'}
          >
            <Sliders size={15} />
            <span>Correct balance</span>
          </button>
        </div>
      </div>

      {/* SECTION 2: PAY SECTION */}
      <div className="card" style={{ marginBottom: '24px' }}>
        <h2 className="section-title">Pay with this Pocket</h2>
        <p className="section-desc">Create a dynamic or QR-backed in-app UPI transaction</p>

        {activeTx && activeTx.status === 'PENDING' ? (
          /* Active Pending Transaction Card */
          <div style={{ background: 'var(--color-surface-subtle)', borderRadius: 'var(--radius-lg)', padding: '20px', border: '1px solid var(--color-border)' }}>
            <div className="tx-pending-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                <span className="badge badge-pending">PENDING APPROVAL</span>
                {activeTx.amountLocked && (
                  <span className="badge badge-locked">
                    <Lock size={12} /> Merchant Locked
                  </span>
                )}
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem', fontWeight: 600, color: 'var(--color-warning)' }}>
                <Clock size={15} />
                <span>{timeRemaining || '10m 00s'}</span>
              </div>
            </div>

            <div style={{ fontSize: '2rem', fontWeight: 800, margin: '8px 0', color: 'var(--color-text-primary)' }}>
              {formatCurrency(activeTx.amount)}
            </div>

            <div style={{ fontSize: '0.9rem', color: 'var(--color-text-secondary)', marginBottom: '20px' }}>
              <div>Payee: <strong>{activeTx.payeeName ? `${activeTx.payeeName} (${activeTx.payeeUpiId})` : activeTx.payeeUpiId}</strong></div>
              {activeTx.note && <div>Note: {activeTx.note}</div>}
            </div>

            {/* Action Buttons */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <button
                onClick={() => handlePayViaUpi(activeTx.id)}
                className="btn btn-primary btn-block"
                disabled={generatingLink}
              >
                <ExternalLink size={16} />
                <span>Pay via UPI App</span>
              </button>

              {isIOS && (
                <div style={{ fontSize: '0.78rem', color: 'var(--color-primary)', textAlign: 'center', fontWeight: 500 }}>
                  If this doesn't open the right app, try a specific app below
                </div>
              )}

              <div style={{ fontSize: '0.76rem', color: 'var(--color-text-muted)', textAlign: 'center' }}>
                Note: Hands off to Default UPI app on your mobile device.
              </div>

              {/* Specific UPI Apps Row */}
              <div className="specific-apps-container">
                <div className="specific-apps-title">Or pay with a specific app</div>
                <div className="specific-apps-grid">
                  <button
                    type="button"
                    onClick={() => handlePayViaSpecificApp('gpay', activeTx.id)}
                    className={`btn-app-pay btn-gpay ${isIOS ? 'ios-prominent' : ''}`}
                    disabled={generatingLink}
                    title="Pay with Google Pay"
                  >
                    <GPayLogo />
                    <span>Google Pay</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => handlePayViaSpecificApp('phonepe', activeTx.id)}
                    className={`btn-app-pay btn-phonepe ${isIOS ? 'ios-prominent' : ''}`}
                    disabled={generatingLink}
                    title="Pay with PhonePe"
                  >
                    <PhonePeLogo />
                    <span>PhonePe</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => handlePayViaSpecificApp('paytm', activeTx.id)}
                    className={`btn-app-pay btn-paytm ${isIOS ? 'ios-prominent' : ''}`}
                    disabled={generatingLink}
                    title="Pay with Paytm"
                  >
                    <PaytmLogo />
                    <span>Paytm</span>
                  </button>
                </div>
                <div className="app-hint-text">
                  Don't have this app installed? Try another option.
                </div>
              </div>

              <div className="tx-pending-actions">
                <button
                  onClick={() => handleConfirmPayment(activeTx.id)}
                  className="btn btn-secondary btn-block"
                  style={{ backgroundColor: 'var(--color-success-bg)', color: 'var(--color-success)', borderColor: 'transparent' }}
                  disabled={confirmingTxId === activeTx.id}
                >
                  <CheckCircle2 size={16} />
                  <span>{confirmingTxId === activeTx.id ? 'Confirming...' : 'Confirm Payment'}</span>
                </button>
                <button
                  onClick={() => handleCancelPayment(activeTx.id)}
                  className="btn btn-secondary btn-block"
                  disabled={cancellingTxId === activeTx.id}
                >
                  <XCircle size={16} />
                  <span>{cancellingTxId === activeTx.id ? 'Cancelling...' : 'Cancel'}</span>
                </button>
              </div>
            </div>
          </div>
        ) : (
          /* Payment Initiation Tabs & Views */
          <div>
            <div className="auth-tabs" style={{ maxWidth: '320px', marginBottom: '20px' }}>
              <button
                type="button"
                className={`auth-tab ${payMode === 'manual' ? 'active' : ''}`}
                onClick={() => {
                  setPayMode('manual');
                  setQrPreview(null);
                }}
              >
                Enter UPI ID
              </button>
              <button
                type="button"
                className={`auth-tab ${payMode === 'qr' ? 'active' : ''}`}
                onClick={() => {
                  setPayMode('qr');
                  setQrPreview(null);
                  startCameraStream();
                }}
              >
                Scan QR
              </button>
            </div>

            {payMode === 'manual' ? (
              <form onSubmit={handleCreateManualPayment}>
                <div className="form-group">
                  <label className="form-label">Payee UPI ID</label>
                  <input
                    type="text"
                    className="form-input"
                    placeholder="merchant@okaxis or 9876543210@paytm"
                    value={payeeUpiId}
                    onChange={(e) => setPayeeUpiId(e.target.value)}
                    disabled={hasPendingTransaction}
                    required
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Amount (₹)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    className="form-input"
                    placeholder="250.00"
                    value={payAmount}
                    onChange={(e) => setPayAmount(e.target.value)}
                    disabled={hasPendingTransaction}
                    required
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Note (Optional)</label>
                  <input
                    type="text"
                    className="form-input"
                    placeholder="e.g. Lunch with team"
                    value={payNote}
                    onChange={(e) => setPayNote(e.target.value)}
                    disabled={hasPendingTransaction}
                  />
                </div>

                <button
                  type="submit"
                  className="btn btn-primary"
                  style={{ width: 'auto', padding: '12px 28px' }}
                  disabled={submittingPay || hasPendingTransaction}
                >
                  <Send size={16} />
                  <span>{submittingPay ? 'Processing...' : 'Proceed to Pay'}</span>
                </button>
              </form>
            ) : qrPreview ? (
              /* Step a & d: QR Preview Card before transaction is created */
              <div className="qr-preview-card">
                <div className="qr-preview-header">
                  <div>
                    <div className="qr-preview-merchant">
                      {qrPreview.payeeName || 'UPI Merchant'}
                    </div>
                    <div className="qr-preview-vpa">
                      {qrPreview.payeeUpiId}
                    </div>
                  </div>
                  <span className="badge badge-source" style={{ fontSize: '0.75rem' }}>
                    <QrCode size={13} style={{ marginRight: '4px' }} /> QR Verified
                  </span>
                </div>

                <form onSubmit={handleConfirmAndPayQr}>
                  <div className="form-group" style={{ marginTop: '16px' }}>
                    <label className="form-label">Amount (₹)</label>
                    {qrPreview.isAmountLocked ? (
                      <div>
                        <div style={{ fontSize: '1.85rem', fontWeight: 800, color: 'var(--color-text-primary)' }}>
                          {formatCurrency(qrPreview.embeddedAmount)}
                        </div>
                        <div style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)', marginTop: '4px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                          <Lock size={12} /> Fixed amount requested by merchant QR
                        </div>
                      </div>
                    ) : (
                      <input
                        type="number"
                        step="0.01"
                        min="0.01"
                        className="form-input"
                        placeholder="Enter amount (e.g. 150.00)"
                        value={qrAmountInput}
                        onChange={(e) => setQrAmountInput(e.target.value)}
                        autoFocus
                        required
                      />
                    )}
                  </div>

                  <div className="form-group">
                    <label className="form-label">Note (Optional)</label>
                    <input
                      type="text"
                      className="form-input"
                      placeholder="e.g. Grocery bill"
                      value={qrNoteInput}
                      onChange={(e) => setQrNoteInput(e.target.value)}
                    />
                  </div>

                  <div style={{ display: 'flex', gap: '12px', marginTop: '24px' }}>
                    <button
                      type="button"
                      onClick={handleCancelPreview}
                      className="btn btn-secondary btn-block"
                    >
                      <RotateCcw size={15} />
                      <span>Cancel & Scan Again</span>
                    </button>
                    <button
                      type="submit"
                      className="btn btn-primary btn-block"
                      disabled={submittingPay || hasPendingTransaction}
                    >
                      <Send size={16} />
                      <span>{submittingPay ? 'Creating...' : `Pay ${qrPreview.isAmountLocked ? formatCurrency(qrPreview.embeddedAmount) : qrAmountInput ? formatCurrency(qrAmountInput) : ''}`}</span>
                    </button>
                  </div>
                </form>
              </div>
            ) : (
              /* QR Camera Scanner View */
              <div>
                {cameraError ? (
                  <div style={{ padding: '24px', background: 'var(--color-danger-bg)', color: 'var(--color-danger)', borderRadius: 'var(--radius-md)', marginBottom: '16px', textAlign: 'center' }}>
                    <div style={{ fontWeight: 600, marginBottom: '8px' }}>{cameraError}</div>
                    <button
                      type="button"
                      onClick={startCameraStream}
                      className="btn btn-secondary btn-sm"
                      style={{ marginTop: '8px' }}
                    >
                      <RefreshCw size={15} />
                      <span>Retry Camera</span>
                    </button>
                  </div>
                ) : (
                  <div>
                    <div className="qr-scanner-box">
                      <video ref={videoRef} />
                      <canvas ref={canvasRef} style={{ display: 'none' }} />
                      <div className="qr-overlay" />
                    </div>
                    <p style={{ textAlign: 'center', fontSize: '0.85rem', color: 'var(--color-text-muted)', marginTop: '12px' }}>
                      Point your camera at any UPI QR code. You'll preview the amount and merchant before confirming.
                    </p>
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>

      {/* SECTION 3: LOG A PURCHASE SECTION */}
      <div className="card" style={{ marginBottom: '24px' }}>
        <h2 className="section-title">Log an External Purchase</h2>
        <p className="section-desc">
          Already paid elsewhere? Log it here to keep this pocket's balance accurate.
        </p>

        <form onSubmit={handleLogPurchase} className="log-purchase-form">
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Amount (₹)</label>
            <input
              type="number"
              step="0.01"
              min="0.01"
              className="form-input"
              placeholder="e.g. 150.00"
              value={logAmount}
              onChange={(e) => setLogAmount(e.target.value)}
              disabled={hasPendingTransaction}
              required
            />
          </div>

          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Note / Description</label>
            <input
              type="text"
              className="form-input"
              placeholder="e.g. Paid in cash at bakery"
              value={logNote}
              onChange={(e) => setLogNote(e.target.value)}
              disabled={hasPendingTransaction}
            />
          </div>

          <button
            type="submit"
            className="btn btn-secondary"
            style={{ height: '48px', padding: '0 24px' }}
            disabled={submittingLog || hasPendingTransaction}
          >
            <span>{submittingLog ? 'Logging...' : 'Log Purchase'}</span>
          </button>
        </form>
      </div>

      {/* SECTION 4: TRANSACTION HISTORY */}
      <div className="card">
        <h2 className="section-title">Transaction History</h2>
        <p className="section-desc">Past in-app payments and manual deductions for this pocket</p>

        {transactions.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-muted)' }}>
            No transactions recorded yet for this pocket.
          </div>
        ) : (
          <div className="tx-list">
            {transactions.map((tx) => (
              <div key={tx.id} className="tx-row">
                <div className="tx-left">
                  <div className="tx-icon">
                    {tx.source === 'MANUAL_LOG' ? <Sliders size={20} /> : <Send size={20} />}
                  </div>
                  <div className="tx-details">
                    <div className="tx-title">
                      {tx.payeeName ? tx.payeeName : tx.payeeUpiId ? tx.payeeUpiId : tx.note || 'Purchase'}
                    </div>
                    <div className="tx-meta">
                      <span>{new Date(tx.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}</span>
                      {tx.note && tx.payeeUpiId && <span>• {tx.note}</span>}
                    </div>
                  </div>
                </div>

                <div className="tx-right">
                  <div className="tx-amount">
                    -{formatCurrency(tx.amount)}
                  </div>
                  <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
                    {tx.amountLocked && (
                      <span className="badge badge-locked" title="Amount locked by merchant QR">
                        <Lock size={10} /> Merchant Locked
                      </span>
                    )}
                    <span className="badge badge-source">{tx.source}</span>
                    <span className={`badge badge-${tx.status.toLowerCase()}`}>
                      {tx.status}
                    </span>
                  </div>

                  {/* Inline Confirm / Cancel action buttons for PENDING transactions */}
                  {tx.status === 'PENDING' && (
                    <div className="tx-inline-actions">
                      <button
                        onClick={() => handleConfirmPayment(tx.id)}
                        className="btn-inline-confirm"
                        disabled={confirmingTxId === tx.id}
                        title="Confirm this payment"
                      >
                        <CheckCircle2 size={13} />
                        <span>{confirmingTxId === tx.id ? '...' : 'Confirm'}</span>
                      </button>
                      <button
                        onClick={() => handleCancelPayment(tx.id)}
                        className="btn-inline-cancel"
                        disabled={cancellingTxId === tx.id}
                        title="Cancel this payment"
                      >
                        <XCircle size={13} />
                        <span>{cancellingTxId === tx.id ? '...' : 'Cancel'}</span>
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Edit Pocket Modal */}
      {isEditModalOpen && (
        <div className="modal-overlay" onClick={() => setIsEditModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">Edit Pocket</h2>
              <button onClick={() => setIsEditModalOpen(false)} className="btn btn-ghost btn-sm">
                <X size={18} />
              </button>
            </div>
            <form onSubmit={handleEditPocket}>
              <div className="form-group">
                <label className="form-label">Pocket Name</label>
                <input
                  type="text"
                  className="form-input"
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  required
                />
              </div>
              <div className="form-group">
                <label className="form-label">Monthly Limit (₹)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  className="form-input"
                  value={editLimit}
                  onChange={(e) => setEditLimit(e.target.value)}
                  required
                />
              </div>
              <div style={{ display: 'flex', gap: '12px', marginTop: '24px' }}>
                <button
                  type="button"
                  onClick={() => setIsEditModalOpen(false)}
                  className="btn btn-secondary btn-block"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary btn-block"
                  disabled={savingEdit}
                >
                  {savingEdit ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Correct Balance Modal */}
      {isOverrideModalOpen && (
        <div className="modal-overlay" onClick={() => setIsOverrideModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">Correct Balance</h2>
              <button onClick={() => setIsOverrideModalOpen(false)} className="btn btn-ghost btn-sm">
                <X size={18} />
              </button>
            </div>
            <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.9rem', marginBottom: '20px' }}>
              Set a manual balance for this pocket (between ₹0 and monthly limit {formatCurrency(pocket.monthlyLimit)}).
            </p>
            <form onSubmit={handleOverrideBalance}>
              <div className="form-group">
                <label className="form-label">New Balance (₹)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.00"
                  max={pocket.monthlyLimit}
                  className="form-input"
                  value={overrideBalanceInput}
                  onChange={(e) => setOverrideBalanceInput(e.target.value)}
                  autoFocus
                  required
                />
              </div>
              <div style={{ display: 'flex', gap: '12px', marginTop: '24px' }}>
                <button
                  type="button"
                  onClick={() => setIsOverrideModalOpen(false)}
                  className="btn btn-secondary btn-block"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary btn-block"
                  disabled={savingOverride}
                >
                  {savingOverride ? 'Updating...' : 'Update Balance'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {isDeleteModalOpen && (
        <div className="modal-overlay" onClick={() => setIsDeleteModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">Delete Pocket?</h2>
              <button onClick={() => setIsDeleteModalOpen(false)} className="btn btn-ghost btn-sm">
                <X size={18} />
              </button>
            </div>
            <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.95rem', lineHeight: '1.5', marginBottom: '24px' }}>
              Are you sure you want to delete <strong>{pocket.name}</strong>? All associated transactions will be permanently deleted.
            </p>
            <div style={{ display: 'flex', gap: '12px' }}>
              <button
                type="button"
                onClick={() => setIsDeleteModalOpen(false)}
                className="btn btn-secondary btn-block"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleDeletePocket}
                className="btn btn-danger btn-block"
                disabled={deleting}
              >
                {deleting ? 'Deleting...' : 'Yes, Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
