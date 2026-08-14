import React from 'react';

interface LoadingSpinnerProps {
  text?: string;
  size?: 'small' | 'medium' | 'large';
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({ text = 'Loading...', size = 'medium' }) => {
  return (
    <div className={`spinner-container ${size}`}>
      <div className="neural-spinner">
        <div className="spinner-ring"></div>
        <div className="spinner-core"></div>
      </div>
      {text && <p className="spinner-text">{text}</p>}
    </div>
  );
};

export default LoadingSpinner;
