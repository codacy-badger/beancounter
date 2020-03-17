import React from "react";
import { Link } from "react-router-dom";
import { useHistory } from "react-router";
import { usePortfolios } from "./hooks";
import ErrorPage from "../common/errors/ErrorPage";
import { useKeycloak } from "@react-keycloak/razzle";

export function Portfolios(): React.ReactElement {
  const [keycloak] = useKeycloak();
  const [portfolios, error] = usePortfolios(keycloak);
  const history = useHistory();

  // Render where we are in the initialization process
  if (!portfolios) {
    return <div id="root">Loading...</div>;
  }
  if (error) {
    return ErrorPage(error.stack, error.message);
  }

  if (portfolios) {
    if (portfolios.length > 0) {
      return (
        <div>
          <nav className="container has-background-grey-lighter">
            <div className="column is-left">
              <button
                className="navbar-item button is-link is-small"
                onClick={() => {
                  return history.push("/portfolio/new");
                }}
              >
                Create
              </button>
            </div>
          </nav>
          <div className="page-box is-primary has-background-light">
            <div className="container">
              <table className={"table is-striped is-hoverable"}>
                <thead>
                  <tr>
                    <th>Code</th>
                    <th>Name</th>
                    <th>Report</th>
                    <th>Base</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {portfolios.map(portfolio => (
                    <tr key={portfolio.id}>
                      <td>
                        <Link to={`/holdings/${portfolio.code}`}>{portfolio.code}</Link>
                      </td>
                      <td>{portfolio.name}</td>
                      <td>
                        {portfolio.currency.symbol}
                        {portfolio.currency.code}
                      </td>
                      <td>
                        {portfolio.base.symbol}
                        {portfolio.base.code}
                      </td>
                      <td>
                        <Link className="fa fa-edit" to={`/portfolio/${portfolio.id}`} />
                        <span> </span>
                        <Link
                          className="fa fa-remove has-padding-left-6"
                          to={`/portfolio/delete/${portfolio.id}`}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      );
    }
  }
  return (
    <div id="root">
      You have no portfolios - <Link to={"/portfolio/new"}>create one?</Link>
    </div>
  );
}

export default Portfolios;
