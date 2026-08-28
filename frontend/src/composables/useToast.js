import { useMessage } from "naive-ui";

/** 轻量 toast，替代手写 .toast */
export function useToast() {
  const message = useMessage();
  return (text, type = "info") => {
    const opts = { duration: 2400 };
    if (type === "error") return message.error(text, opts);
    if (type === "success") return message.success(text, opts);
    return message.info(text, opts);
  };
}
