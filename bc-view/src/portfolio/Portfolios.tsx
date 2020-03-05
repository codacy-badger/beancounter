import React, { useEffect, useState } from "react";
import { Portfolio } from "../types/beancounter";
import { AxiosError } from "axios";
import logger from "../common/ConfigLogging";
import { Link } from "react-router-dom";
import handleError from "../common/errors/UserError";
import { _axios, getBearerToken, setToken } from "../common/axiosUtils";
import { useKeycloak } from "@react-keycloak/web";

export default function Portfolios(): React.ReactElement {
  const [portfolios, setPortfolios] = useState<Portfolio[]>();
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<AxiosError>();
  const [keycloak] = useKeycloak();

  useEffect(() => {
    const fetchPortfolios = async (config: {
      headers: { Authorization: string };
    }): Promise<void> => {
      setLoading(true);
      logger.debug(">>fetch apiPortfolios");
      await _axios
        .get<Portfolio[]>("/bff/portfolios", config)
        .then(result => {
          logger.debug("<<fetched apiPortfolios");
          setPortfolios(result.data);
        })
        .catch(err => {
          setError(err);
          if (err.response) {
            logger.error("axios error [%s]: [%s]", err.response.status, err.response.data.message);
          }
        });
    };
    setToken(keycloak);
    fetchPortfolios({
      headers: getBearerToken()
    }).finally(() => setLoading(false));
  }, [keycloak]);
  // Render where we are in the initialization process
  if (loading) {
    return <div id="root">Loading...</div>;
  }
  if (error) {
    return handleError(error, true);
  }
  if (portfolios) {
    if (portfolios.length > 0) {
      return (
        <div className="page-box">
          <table className={"table is-striped is-hoverable"}>
            <tbody>
              {portfolios.map(portfolio => (
                <tr key={portfolio.id}>
                  <td align={"left"}>
                    <Link to={`/holdings/${portfolio.code}`}>{portfolio.code}</Link>
                  </td>
                  <td align={"left"}>{portfolio.name}</td>
                  <td>
                    {portfolio.currency.symbol}
                    {portfolio.currency.code}
                  </td>
                  <td>
                    {portfolio.base.symbol}
                    {portfolio.base.code}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      );
    }
  }
  return <div id="root">You have no portfolios - create one?</div>;
}
