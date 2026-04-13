import { QUESTION_CATEGORIES } from '../../constants/categories';

interface Props {
  value: string;
  onChange: (value: string) => void;
}

export default function CategoryTags({ value, onChange }: Props) {
  return (
    <div className="category-tags">
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
