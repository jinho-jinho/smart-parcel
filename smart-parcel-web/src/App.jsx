// src/App.jsx
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./pages/Login";              // 이미 사용 중인 로그인
import SignUp from "./pages/SignUp";            // 회원가입
import PasswordReset from "./pages/PasswordReset";  // ← 새로 추가

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<SignUp />} />
        <Route path="/reset-password" element={<PasswordReset />} />
      </Routes>
    </BrowserRouter>
  );
}
