import React from 'react';

interface CardProps {
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
  onClick?: () => void;
}

export const Card: React.FC<CardProps> = ({ children, className = '', style, onClick }) => {
  return (
    <div className={`glass-card ${className}`} style={style} onClick={onClick}>
      {children}
    </div>
  );
};

export default Card;
