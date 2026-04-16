type FlowStepProps = {
  step: string;
  title: string;
  description: string;
  bullets: readonly string[];
};

export function FlowStep({ step, title, description, bullets }: FlowStepProps) {
  return (
    <section className="panel flow-step" data-step={step}>
      <h3>{title}</h3>
      <p>{description}</p>
      <ul className="list">
        {bullets.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>
    </section>
  );
}
