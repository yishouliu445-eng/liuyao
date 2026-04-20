export type EntryMode = 'asked' | 'manual';

interface Props {
  value: EntryMode;
  onChange: (value: EntryMode) => void;
}

const OPTIONS: Array<{ value: EntryMode; label: string }> = [
  { value: 'asked', label: '问事' },
  { value: 'manual', label: '手工起卦' },
];

export default function EntryModeTabs({ value, onChange }: Props) {
  return (
    <div className="entry-mode-tabs" role="tablist" aria-label="起问方式">
      {OPTIONS.map((option) => (
        <button
          key={option.value}
          type="button"
          role="tab"
          aria-selected={value === option.value}
          className={`entry-mode-tab ${value === option.value ? 'active' : ''}`}
          onClick={() => onChange(option.value)}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}
