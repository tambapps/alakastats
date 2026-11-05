// The tests opening the real app wait for what it does in the background: more than the 2 seconds Mocha gives by default
config.client = config.client || {};
config.client.mocha = { ...config.client.mocha, timeout: 30000 };
