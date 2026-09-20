import React from "react";
import { cn } from "../utils/cn";

export function Card({
  title,
  actions,
  children,
  className,
}: React.PropsWithChildren<{ title?: string; actions?: React.ReactNode; className?: string }>) {
  return (
    <section className={cn("card", className)}>
      {(title || actions) && (
        <div className="cardHeader">
          <div className="cardTitle">{title}</div>
          <div>{actions}</div>
        </div>
      )}
      <div className="cardBody">{children}</div>
    </section>
  );
}
