import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "智约校园",
  description: "多态空间调度与高并发活动综合平台"
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="zh-CN">
      <body>{children}</body>
    </html>
  );
}
