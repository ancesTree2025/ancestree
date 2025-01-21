<script lang="ts">
  import * as d3 from 'd3';
  import { onMount } from 'svelte';
  
  const data = [
      { id: "parent1", name: "Parent 1", x: 300, y: 100 },
      { id: "parent2", name: "Parent 2", x: 400, y: 100 },
      { id: "child1", name: "Child 1", x: 350, y: 150 },
      { id: "child2", name: "Child 2", x: 450, y: 150 }
  ];

  const links = [
      { source: "parent1", target: "parent2" },
      { source: "midpoint", target: "child1" },
      { source: "midpoint", target: "child2" }
  ];
  // Calculate the midpoint between the parents
  const midpoint = { id: "midpoint", name: "", x: (data[0].x + data[1].x) / 2, y: data[0].y };

  // Add the midpoint to the data
  data.push(midpoint);

  // Add links
  onMount(() => {
    const svg = d3.select("svg"),
      width = +svg.attr("width"),
      height = +svg.attr("height");
    
    svg.selectAll(".link")
      .data(links)
      .enter()
      .append("line")
      .attr("class", "link")
      .attr("x1", d => data.find(node => node.id === d.source)!.x)
      .attr("y1", d => data.find(node => node.id === d.source)!.y)
      .attr("x2", d => data.find(node => node.id === d.target)!.x)
      .attr("y2", d => data.find(node => node.id === d.target)!.y);

    // Add nodes
    const nodes = svg.selectAll(".node")
        .data(data)
        .enter()
        .append("g")
        .attr("class", "node")
        .attr("transform", d => `translate(${d.x},${d.y})`);

    nodes.append("circle")
        .attr("r", d => d.id === "midpoint" ? 0 : 20)
        .attr("class", d => d.id === "midpoint" ? "midpoint" : "");

    nodes.append("text")
        .attr("dy", 5)
        .text(d => d.name || "");
  });
</script>

<svg width="800" height="600"></svg>