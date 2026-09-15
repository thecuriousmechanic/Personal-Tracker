interface ProgressRingProps {
  value: number;
  size?: number;
  strokeWidth?: number;
  label?: string;
}

export function ProgressRing({ value, size = 112, strokeWidth = 8, label = "done" }: ProgressRingProps) {
  const radius = (size - strokeWidth) / 2;
  const circumference = radius * Math.PI * 2;
  const progress = Math.max(0, Math.min(100, value));
  const offset = circumference - (progress / 100) * circumference;

  return (
    <div className="progress-ring" style={{ width: size, height: size }}>
      <svg width={size} height={size} aria-hidden="true">
        <circle className="progress-ring__track" cx={size / 2} cy={size / 2} r={radius} strokeWidth={strokeWidth} />
        <circle
          className="progress-ring__value"
          cx={size / 2}
          cy={size / 2}
          r={radius}
          strokeWidth={strokeWidth}
          strokeDasharray={circumference}
          strokeDashoffset={offset}
        />
      </svg>
      <div className="progress-ring__content">
        <strong>{progress}%</strong>
        <span>{label}</span>
      </div>
    </div>
  );
}
