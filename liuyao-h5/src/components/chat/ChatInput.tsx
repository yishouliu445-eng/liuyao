import { useState } from 'react';

interface Props {
  onSend: (content: string) => Promise<void> | void;
  loading?: boolean;
  placeholder?: string;
  disabled?: boolean;
}

export default function ChatInput({
  onSend,
  loading = false,
  placeholder = '继续追问，或从这里输入你想确认的细节…',
  disabled = false,
}: Props) {
  const [value, setValue] = useState('');

  async function submitCurrentValue() {
    const content = value.trim();
    if (!content || loading || disabled) return;
    await onSend(content);
    setValue('');
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    await submitCurrentValue();
  }

  return (
    <form className="chat-input" onSubmit={handleSubmit}>
      <textarea
        className="chat-input-field"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder={placeholder}
        rows={2}
        disabled={loading || disabled}
        onKeyDown={(e) => {
          if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            void submitCurrentValue();
          }
        }}
      />
      <div className="chat-input-actions">
        <span className="chat-input-hint">Enter 发送 · Shift+Enter 换行</span>
        <button className="chat-send-btn" type="submit" disabled={loading || disabled || !value.trim()}>
          {loading ? '发送中...' : '发送'}
        </button>
      </div>
    </form>
  );
}
