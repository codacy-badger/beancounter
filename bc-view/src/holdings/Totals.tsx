import { Holdings } from "../types/beancounter";
import NumberFormat from "react-number-format";
import React from "react";
import { useTranslation } from "react-i18next";
import i18n from "../ConfigI18n";

export default function Totals(holdings: Holdings): JSX.Element {
  const { t } = useTranslation("translations", { i18n });
  // Transform the contract into the view the user requested
  const valueIn = "BASE";
  return (
    <tbody className={"table-container is-large"}
           key={holdings.portfolio.code + "totals"}
    >
    <tr key={holdings.totals[valueIn].valueIn}>
      <td colSpan={4} align={"right"}>
        Totals
      </td>
      <td align={"right"}>
        <NumberFormat
          value={holdings.totals[valueIn].costValue}
          displayType={"text"}
          decimalScale={2}
          fixedDecimalScale={true}
          thousandSeparator={true}
        />
      </td>
      <td align={"right"}>
        <NumberFormat
          value={holdings.totals[valueIn].marketValue}
          displayType={"text"}
          decimalScale={2}
          fixedDecimalScale={true}
          thousandSeparator={true}
        />
      </td>
      <td align={"right"}>
        <NumberFormat
          value={holdings.totals[valueIn].dividends}
          displayType={"text"}
          decimalScale={2}
          fixedDecimalScale={true}
          thousandSeparator={true}
        />
      </td>
      <td align={"right"}>
        <NumberFormat
          value={holdings.totals[valueIn].realisedGain}
          displayType={"text"}
          decimalScale={2}
          fixedDecimalScale={true}
          thousandSeparator={true}
        />
      </td>
      <td align={"right"}>
        <NumberFormat
          value={holdings.totals[valueIn].unrealisedGain}
          displayType={"text"}
          decimalScale={2}
          fixedDecimalScale={true}
          thousandSeparator={true}
        />
      </td>
      <td align={"right"}>
        <NumberFormat
          value={holdings.totals[valueIn].totalGain}
          displayType={"text"}
          decimalScale={2}
          fixedDecimalScale={true}
          thousandSeparator={true}
        />
      </td>
    </tr>
    </tbody>
  );
}