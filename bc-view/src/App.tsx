import Home from "./Home";
import React from "react";
import { Route, Switch } from "react-router-dom";
import LayoutHoldings from "./components/LayoutHoldings";
import "./App.css";

const App = () => (
  <Switch>
    <Route exact={true} path="/" component={Home} />
    <Route exact={true} path="/holdings" component={LayoutHoldings} />
  </Switch>
);

export default App;
