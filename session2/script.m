%%
% Diktya 1 Doinakis Michalis 9292
% e-mail: doinakis@ece.auth.gr
% Session2 script
%%
clc
clear
close all
%%
% Request Codes
echo = "E4511.";
ack = "Q9912,";
nack = "R1580.";
%%
% Produces graph G1 with the response times in milliseconds for a 4 minute
% experiment
%%
echoExperiment = importdata("echoExperiment.csv");
G1 = figure;
bar(echoExperiment);
g1Title = "G1: Echo Request Code: " + echo + newline + "Echo Packet experiment started: 2021-03-31 at 19:50:38 EEST" + ...
    newline + "Echo Packet experiment ended: 2021-03-31 at 19:54:38 EEST.";
title(g1Title);
ylabel("Response time (in milliseconds)");
xlabel("Packet");
saveas(G1,"G1.jpg");
%%
% Produces graph G2 with the response times in milliseconds for a 4 minute
% experiment (Automatic Repeat Request)
arqResponseTimes = importdata("arqResponseTimes.csv");
G2 = figure;
bar(arqResponseTimes);
g2Title = "G2: ACK Request Code: " + ack + "NACK Request Code: " + nack + newline + ...
    "Automatic Repeat experiment started: 2021-03-31 at 19:55:07 EEST" + newline + ...
    "Automatic Repeat Request experiment ended: 2021-03-31 at 19:59:07 EEST.";
title(g2Title);
ylabel("Response time (in milliseconds)");
xlabel("Packet");
saveas(G2,"G2.jpg");
%%
% Finds the estimation of the distribution for the repeations
ArqNumberOfNack = importdata("ArqNumberOfNack.csv");
ArqNumberOfNack = ArqNumberOfNack(ArqNumberOfNack ~=0);
timesRequested = unique(ArqNumberOfNack);
pd = fitdist(ArqNumberOfNack','Exponential');
xgrid = linspace(0,5,100)';
pdfEst = pdf(pd,xgrid);
G3 = figure;
h = histogram(ArqNumberOfNack,'Normalization','pdf');
line(xgrid+1,pdfEst);
ylim([0 1]);
xticks(1:size(timesRequested,2));
g3Title = "Distribution estimation." + newline + "G3: ACK Request Code: " + ack + "NACK Request Code: " + nack + ...
    newline + "Automatic Repeat experiment started: 2021-03-31 at 19:55:07 EEST" + newline + ...
    "Automatic Repeat Request experiment ended: 2021-03-31 at 19:59:07 EEST";
title(g3Title);
xlabel("#Repeations");
ylabel("Relative frequency of repeations");
saveas(G3,"G3.jpg");
%%
% Calculation of Bit Error Rate
% P = (1-BER)^L <=> BER = 1 - P^(1/L)
%%
ArqNumberOfNack = importdata("ArqNumberOfNack.csv");
k = 0;
L = 16 * 8;
ArqNumberOfNack = ArqNumberOfNack(ArqNumberOfNack ~=0);
timesRequested = unique(ArqNumberOfNack);

% k = 1*times(1) + 2*times(2) + ... + n*times(n)
for i= 1:size(timesRequested,2)
    if timesRequested(i) == 0
        continue;
    end
    k = k + timesRequested(i) * sum(ArqNumberOfNack == timesRequested(i));
end

P = size(ArqNumberOfNack,2)/k;
BER = 1 - P^(1/L);
%%