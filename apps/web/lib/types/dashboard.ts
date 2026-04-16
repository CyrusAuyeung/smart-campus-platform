export type MetricCard = {
  label: string;
  value: string;
  hint: string;
};

export type DashboardSummary = {
  title: string;
  subtitle: string;
  metrics: MetricCard[];
  highlights: string[];
};
