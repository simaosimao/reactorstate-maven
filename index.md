---
---
## Maven Documentation
{% assign mavendocs = site.collections | where: "label", "mavendocs" | first -%}
{% assign collectionBasePath = mavendocs.label -%}
{% assign indexes = mavendocs.files | where: "name", "index.html" | sort: "path" | reverse -%}
{% for index in indexes -%}
{%     assign pathParts = index.path | split: "/" -%}
{%     if pathParts.size == 3 -%}
{%         assign version = pathParts[1] -%}
* [Version {{ version }}]({{ collectionBasePath }}/{{ version }}/)
{%      endif -%}
{% endfor -%}
