import React from 'react';
import { LucideIcon } from 'lucide-react';

interface EmptyStateProps {
  icon?: LucideIcon;
  title: string;
  description?: string;
  actionText?: string;
  onAction?: () => void;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon: Icon,
  title,
  description,
  actionText,
  onAction,
}) => {
  return (
    <div className="empty-state-card">
      {Icon && (
        <div className="empty-icon-wrapper">
          <Icon size={40} className="empty-icon" />
        </div>
      )}
      <h3 className="empty-title">{title}</h3>
      {description && <p className="empty-description">{description}</p>}
      {actionText && onAction && (
        <button onClick={onAction} className="btn btn-primary btn-md margin-top-md">
          {actionText}
        </button>
      )}
    </div>
  );
};

export default EmptyState;
