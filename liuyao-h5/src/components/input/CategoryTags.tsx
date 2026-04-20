import { QUESTION_CATEGORIES } from '../../constants/categories';

interface Props {
  value: string;
  onChange: (value: string) => void;
  allowUnset?: boolean;
  unsetLabel?: string;
}

export default function CategoryTags({
  value,
  onChange,
  allowUnset = false,
  unsetLabel = '暂不指定',
}: Props) {
  return (
    <div className="category-tags">
      {allowUnset && (
        <button
          type="button"
          className={`category-tag ${!value ? 'active' : ''}`}
          onClick={() => onChange('')}
        >
          {unsetLabel}
        </button>
      )}
      {QUESTION_CATEGORIES.map((cat) => (
        <button
          key={cat.value}
          type="button"
          className={`category-tag ${value === cat.value ? 'active' : ''}`}
          onClick={() => onChange(cat.value)}
        >
          {cat.label}
        </button>
      ))}
    </div>
  );
}
