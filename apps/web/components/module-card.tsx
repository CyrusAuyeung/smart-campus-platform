type ModuleCardProps = {
  title: string;
  description: string;
  highlights: readonly string[];
};

export function ModuleCard({ title, description, highlights }: ModuleCardProps) {
  return (
    <article className="card">
      <h3>{title}</h3>
      <p>{description}</p>
      <ul className="list">
        {highlights.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>
    </article>
  );
}
