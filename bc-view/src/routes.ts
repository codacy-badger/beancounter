import Home from "./Home";
import ViewHoldings from "./holdings";
import { useParams } from "react-router-dom";
import Login from "./common/auth/Login";
import Logout from "./common/auth/Logout";
import Portfolios from "./portfolio/Portfolios";

const RouteHoldings = (): JSX.Element => {
  const { portfolioId } = useParams();
  if (portfolioId) return ViewHoldings(portfolioId);
  return ViewHoldings("portfolioId");
};

const Routes = [
  {
    path: "/",
    exact: true,
    component: Home
  },
  {
    path: "/login",
    component: Login
  },
  {
    path: "/logout",
    component: Logout
  },
  {
    path: "/portfolios",
    component: Portfolios
  },

  {
    path: "/holdings/:portfolioId",
    component: RouteHoldings
  }
];

export default Routes;
