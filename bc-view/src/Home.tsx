import React from "react";

import * as logo from "./react.svg";

function Home(): JSX.Element {
  return (
    <div className="Home">
      <div className="Home-header">
        <img src={logo} className="Home-logo" alt="logo" />
        <h2>Welcome to Beancounter</h2>
      </div>
      <p className="Home-intro">
        To get started, edit <code>src/App.js</code> or <code>src/Home.js</code>{" "}
        and save to reload.
      </p>
      <ul className="Home-resources">
        <li>
          <a href="https://github.com/jaredpalmer/razzle">Docs</a>
        </li>
        <li>
          <a href="https://github.com/jaredpalmer/razzle/issues">Issues</a>
        </li>
        <li>
          <a href="https://palmer.chat">Community Slack</a>
        </li>
      </ul>
    </div>
  );
}

export default Home;
