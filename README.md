# Title

example:

- mcp.build.setting.json
```json5 
{
  "placeholder": {
    // running file suffixes
    "suffixes": [
      ".yml"
    ],
    "data": {
      // placeholder root
      "global": {
        "port": 3306,
        "password": "$(random.uuid)"
      },
      "port": "$(mcp.global.port)(number)",
      "password": "$(mcp.global.password)"
    }
  }
}
```

- /default/files/init.yml
```yaml
mysql:
  # if use $(mcp.port) will error
  # use (number) to remove "
  port: "$(mcp.port)(number)"
  password: "$(mcp.password)"
```

```shell
java -cp MCServerPacker-1.0-SNAPSHOT-all.jar fun.xiantiao.mcpacker.Main
```

look `built` folder

## error info

### Invalid path

Q: `Exception in thread "main" java.lang.IllegalArgumentException: Invalid path: placeholder.data.portPor`  
A: check your file in `default` folder 
