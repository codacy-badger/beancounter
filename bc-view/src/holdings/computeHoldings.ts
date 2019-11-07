import { HoldingContract, Holdings, MoneyValues, Position } from "../types/beancounter";
import { GroupBy, ValuationCcy } from "./enums";

function getPath(path: string, position: Position): string {
  return (path
    .split(".")
    .reduce(
      (p, c) => (p && p[c]) || "undefined",
      position
    ) as unknown) as string;
}

function total(
  total: MoneyValues,
  position: Position,
  valueIn: ValuationCcy
): MoneyValues {
  if (!total) {
    total = {
      costValue: 0,
      dividends: 0,
      marketValue: 0,
      realisedGain: 0,
      totalGain: 0,
      unrealisedGain: 0,
      fees: 0,
      purchases: 0,
      sales: 0,
      costBasis: 0,
      valueIn: valueIn,
      averageCost: 0,
      price: 0,
      currency: position.moneyValues[valueIn].currency
    };
  }
  total.marketValue += position.moneyValues[valueIn].marketValue;
  total.costValue = total.costValue + position.moneyValues[valueIn].costValue;
  total.dividends = total.dividends + position.moneyValues[valueIn].dividends;
  total.realisedGain =
    total.realisedGain + position.moneyValues[valueIn].realisedGain;
  total.unrealisedGain =
    total.unrealisedGain + position.moneyValues[valueIn].unrealisedGain;
  total.totalGain = total.totalGain + position.moneyValues[valueIn].totalGain;

  return total;
}

function totals(
  totals: MoneyValues[],
  position: Position,
  valueIn: ValuationCcy
): MoneyValues[] {
  if (!totals) {
    totals = [];
  }
  totals[valueIn] = total(totals[valueIn], position, valueIn);
  return totals;
}
// Transform the holdingContract into Holdings suitable for display
export function computeHoldings(
  contract: HoldingContract,
  hideEmpty: boolean,
  valueIn: ValuationCcy,
  groupBy: GroupBy
): Holdings {
  return Object.keys(contract.positions)
    .filter(positionKey =>
      hideEmpty
        ? contract.positions[positionKey].quantityValues.total !== 0
        : true
    )
    .reduce(
      (results: Holdings, group) => {
        const position = contract.positions[group] as Position;
        const groupKey = getPath(groupBy, position);

        results.holdingGroups[groupKey] = results.holdingGroups[groupKey] || {
          group: groupKey,
          positions: [],
          total: 0
        };
        results.totals["PORTFOLIO"] = results.totals["PORTFOLIO"] || {};
        results.holdingGroups[groupKey].positions.push(position);
        results.totals["PORTFOLIO"] = total(
          results.totals["PORTFOLIO"],
          position,
          "PORTFOLIO"
        );
        results.totals["BASE"] = total(
          results.totals["BASE"],
          position,
          "BASE"
        );
        results.holdingGroups[groupKey].subTotals = totals(
          results.holdingGroups[groupKey].subTotals,
          position,
          valueIn
        );

        results.valueIn = valueIn;
        return results;
      },
      {
        portfolio: contract.portfolio,
        holdingGroups: [],
        valueIn: valueIn,
        totals: []
      }
    );
}