import type { DirectionResolutionDTO } from '../../types/session';

interface Props {
  resolution: DirectionResolutionDTO | null;
  onConfirmSuggested: () => void;
  onKeepSelected: () => void;
  onClose: () => void;
}

export default function DirectionConfirmationDialog({
  resolution,
  onConfirmSuggested,
  onKeepSelected,
  onClose,
}: Props) {
  if (!resolution) {
    return null;
  }

  return (
    <div className="dialog-backdrop" role="presentation" onClick={onClose}>
      <div
        className="dialog-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="direction-confirmation-title"
        onClick={(event) => event.stopPropagation()}
      >
        <span className="entry-kicker">方向待确认</span>
        <h2 id="direction-confirmation-title" className="dialog-title">
          你选择的是“{resolution.userSelectedDirection || '未改定'}”，
          但问题更像“{resolution.suggestedDirection || resolution.detectedDirection}”。
        </h2>
        <p className="dialog-text">请先定下这一问的解读方向，再继续进入问事。</p>
        <div className="dialog-actions">
          <button type="button" className="dialog-primary" onClick={onConfirmSuggested}>
            按{resolution.suggestedDirection || resolution.detectedDirection}解读
          </button>
          <button type="button" className="dialog-secondary" onClick={onKeepSelected}>
            仍按{resolution.userSelectedDirection || '当前选择'}
          </button>
        </div>
      </div>
    </div>
  );
}
