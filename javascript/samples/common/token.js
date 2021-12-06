const KEY = "KEY";
const SECRET = "SECRET";

const applicationcreds = (authtype) => {
  const queryParams = {};
  const args = window.location.search.substr(1).split(/&/);
  for (let i = 0; i < args.length; i += 1) {
    const tmp = args[i].split(/=/);
    if (tmp[0] !== "") {
      queryParams[decodeURIComponent(tmp[0])] = decodeURIComponent(
        tmp.slice(1).join("").replace("+", " ")
      );
      const ks = atob(`${queryParams.t}`).split(":");

      if (authtype === KEY) {
        const key = ks[0].toString();
        return key;
      }
      if (authtype === SECRET) {
        const secret = ks[1].toString().trim();
        return secret;
      }
    }
  }
  throw Error("authtype not supported");
};

export const getKey = () => applicationcreds(KEY);
export const getSecret = () => applicationcreds(SECRET);
