interface Props {
  prompts: string[];
  onPick: (prompt: string) => void;
  disabled?: boolean;
}

export default function SmartPromptBar({ prompts, onPick, disabled = false }: Props) {
  if (!prompts.length) return null;

  return (
    <div className="smart-prompt-bar">
      <span className="smart-prompt-label">快捷追问</span>
      <div className="smart-prompt-list">
        {prompts.map((prompt) => (
          <button
            key={prompt}
            type="button"
            className="smart-prompt-chip"
            onClick={() => onPick(prompt)}
            disabled={disabled}
          >
            {prompt}
          </button>
        ))}
      </div>
    </div>
  );
}
